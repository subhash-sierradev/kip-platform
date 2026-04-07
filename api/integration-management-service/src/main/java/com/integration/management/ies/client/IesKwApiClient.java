package com.integration.management.ies.client;

import com.integration.execution.contract.rest.response.kaseware.KwDocField;
import com.integration.execution.contract.rest.response.kaseware.KwItemSubtypeDto;
import com.integration.execution.contract.rest.response.kaseware.KwDynamicDocType;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

@FeignClient(name = "execution-kw-doc")
public interface IesKwApiClient {

    @Cacheable(value = "kwDynamicDocTypeCache", key = "#type + ':' + #subType")
    @GetMapping("/api/dynamic-documents-types")
    List<KwDynamicDocType> getDynamicDocuments(@RequestParam String type, @RequestParam String subType);

    @Cacheable(value = "kwItemSubtypesCache")
    @GetMapping("/api/item-subtypes")
    List<KwItemSubtypeDto> getItemSubtypes();

    @Cacheable(value = "kwDocFieldsCache")
    @GetMapping("/api/source-field-mappings")
    List<KwDocField> getSourceFieldMappings();
}
