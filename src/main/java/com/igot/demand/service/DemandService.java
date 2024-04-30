package com.igot.demand.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.igot.demand.dto.CustomResponse;
import com.igot.demand.elasticsearch.dto.SearchCriteria;

public interface DemandService {
    CustomResponse createDemand(JsonNode demandDetails);

    CustomResponse readDemand(String id);

    CustomResponse searchDemand(SearchCriteria searchCriteria);

    String delete(String id);

}
