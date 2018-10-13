package cz.cvut.kbss.termit.rest;

import cz.cvut.kbss.jsonld.JsonLd;
import cz.cvut.kbss.termit.model.Term;
import cz.cvut.kbss.termit.service.IdentifierResolver;
import cz.cvut.kbss.termit.service.LanguageService;
import cz.cvut.kbss.termit.util.Configuration;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/language")
public class LanguageController extends BaseController {

    private static final Logger LOG = LoggerFactory.getLogger(LanguageController.class);

    private final LanguageService service;

    @Autowired
    public LanguageController(
        IdentifierResolver idResolver,
        Configuration config,
        LanguageService service) {
        super(idResolver, config);
        this.service = service;
    }

    /**
     * @return List of types
     */
    @RequestMapping(value = "/types", method = RequestMethod.GET,
            produces = {MediaType.APPLICATION_JSON_VALUE, JsonLd.MEDIA_TYPE})
    public List<Term> getAll(@RequestParam String language) {
        return service.findAll(language);
    }
}
