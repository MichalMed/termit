package cz.cvut.kbss.termit.rest;

import cz.cvut.kbss.jsonld.JsonLd;
import cz.cvut.kbss.termit.service.IdentifierResolver;
import cz.cvut.kbss.termit.service.repository.QueryService;
import cz.cvut.kbss.termit.util.Configuration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/query")
public class QueryController extends BaseController {

    private final QueryService queryService;

    @Autowired
    public QueryController(QueryService queryService, IdentifierResolver idResolver,
                           Configuration config) {
        super(idResolver, config);
        this.queryService = queryService;
    }

    @RequestMapping(method = RequestMethod.GET,
                    produces = {MediaType.APPLICATION_JSON_VALUE, JsonLd.MEDIA_TYPE})
    public String query(@RequestParam("queryString")
                            final String queryString) {
        return queryService.query(queryString);
    }
}