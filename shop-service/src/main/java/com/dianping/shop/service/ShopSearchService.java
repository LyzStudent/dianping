package com.dianping.shop.service;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.StrUtil;
import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.SortOrder;
import co.elastic.clients.elasticsearch._types.query_dsl.BoolQuery;
import co.elastic.clients.elasticsearch._types.query_dsl.MultiMatchQuery;
import co.elastic.clients.elasticsearch._types.query_dsl.Query;
import co.elastic.clients.elasticsearch._types.query_dsl.TermQuery;
import co.elastic.clients.elasticsearch.core.SearchResponse;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.dianping.common.dto.Result;
import com.dianping.shop.document.ShopDocument;
import com.dianping.shop.entity.Shop;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Service
public class ShopSearchService {

    private static final String INDEX="shop";

    @Resource
    private ElasticsearchClient client;

    @Resource
    private ShopService shopService;

    /**
     * 创建索引
     */
    public void ensureIndex() throws IOException {
        boolean exists=client.indices().exists(e->e.index(INDEX)).value();
        if(exists) return;

        String mapping="""
            {
              "settings": { "number_of_shards": "1", "number_of_replicas": "0" },
              "mappings": {
                "properties": {
                  "id":      { "type": "keyword" },
                  "name":    { "type": "text", "analyzer": "ik_max_word", "search_analyzer": "ik_smart" },
                  "area":    { "type": "text", "analyzer": "ik_max_word", "search_analyzer": "ik_smart" },
                  "address": { "type": "text", "analyzer": "ik_max_word", "search_analyzer": "ik_smart" },
                  "typeId":  { "type": "keyword" },
                  "avgPrice":{ "type": "long" },
                  "sold":    { "type": "integer" },
                  "comments":{ "type": "integer" },
                  "score":   { "type": "integer" },
                  "x":       { "type": "double" },
                  "y":       { "type": "double" },
                  "openTime":{ "type": "keyword", "index": false },
                  "images":  { "type": "keyword", "index": false }
                }
              }
            }
            """;
        client.indices().create(c->c.index(INDEX)
                .withJson(new ByteArrayInputStream(mapping.getBytes(StandardCharsets.UTF_8))));
        log.info("ES 索引 {} 创建成功",INDEX);
    }

    /**
     * 关键词搜索+类型筛选+高亮+排序分页
     */
    public Result search(String keyword, Long typeId, int page, int size){
        try{
            int from=Math.max(0,(page-1)*size);
            List<Query> must=new ArrayList<>();

            if(StrUtil.isNotBlank(keyword)){
                //name权重最高，area/address次之
                must.add(MultiMatchQuery.of(m->m.fields("name^3","area","address")
                        .query(keyword))._toQuery());
            }
            if(typeId!=null){
                must.add(TermQuery.of(t-> t.field("typeId").value(typeId))._toQuery());
            }

            SearchResponse<ShopDocument> response= client.search(s->s.index(INDEX)
                    .query(BoolQuery.of(b->b.must(must))._toQuery())
                    .highlight(h->h.fields("name",f->f.preTags("<em>").postTags("</em>")))
                    .sort(so->so.field(f->f.field("score").order(SortOrder.Desc)))
                    .sort(so->so.field(f->f.field("sold").order(SortOrder.Desc)))
                    .from(from)
                    .size(size), ShopDocument.class);

            //高亮回填name
            List<ShopDocument> list=response.hits().hits().stream().map(hit->{
                ShopDocument doc=hit.source();
                if(doc!=null){
                    Map<String,List<String>> hl=hit.highlight();
                    if(hl!=null&&hl.containsKey("name")&&!hl.get("name").isEmpty()){
                        doc.setName(hl.get("name").get(0));
                    }
                }
                return doc;
            }).collect(Collectors.toList());

            //过滤掉未上架店铺（ES 文档不存 status，回库过滤）
            List<Long> ids=list.stream().map(ShopDocument::getId).toList();
            if(!ids.isEmpty()){
                Set<Long> visible=shopService.listByIds(ids).stream()
                        .filter(s->s.getStatus()!=null&&s.getStatus()==1)
                        .map(Shop::getId).collect(Collectors.toSet());
                list=list.stream().filter(d->visible.contains(d.getId())).collect(Collectors.toList());
            }

            long total=response.hits().total()==null?0:response.hits().total().value();
            return Result.ok(list,total);
        }catch (IOException e){
            log.error("ES 搜索失败",e);
            return Result.fail("搜索服务异常");
        }
    }

    /**
     * 单条同步（店铺保存/更新后调用，先查库保证是完整数据）
     */
    public void indexById(Long id){
        try{
            Shop shop=shopService.getById(id);
            if(shop==null) return;
            ShopDocument doc= BeanUtil.copyProperties(shop, ShopDocument.class);
            client.index(i->i.index(INDEX).id(String.valueOf(id)).document(doc));
        }catch (IOException e){
            log.error("ES 索引写入失败，id= {}",id,e);
        }
    }

    /**
     * 全局重建（分页读库批量写）
     */
    public void syncAll(){
        try{
            ensureIndex();
            long page=1;
            while (true){
                Page<Shop> p=shopService.page(new Page<>(page,200));
                List<Shop> records=p.getRecords();
                if(records.isEmpty()) break;
                for(Shop shop:records){
                    ShopDocument doc=BeanUtil.copyProperties(shop, ShopDocument.class);

                    client.index(i->i.index(INDEX)
                            .id(String.valueOf(shop.getId())).document(doc));
                }
                if(page>=p.getPages()) break;
                page++;
            }
            log.info("ES 全量同步完成");
        }catch (IOException e){
            log.error("ES 全量同步失败",e);
        }
    }

    /**
     * 从ES删除店铺（下架时调用）
     * @param id
     */
    public void deleteById(Long id){
        try{
            client.delete(d->d.index(INDEX).id(String.valueOf(id)));
        }catch (IOException e){
            log.error("ES 删除失败 id= {}",id,e);
        }
    }

}
