package com.igot.demand.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.TextNode;
import com.igot.demand.cache.CacheService;
import com.igot.demand.dto.CustomResponse;
import com.igot.demand.dto.RespParam;
import com.igot.demand.elasticsearch.dto.SearchCriteria;
import com.igot.demand.elasticsearch.dto.SearchResult;
import com.igot.demand.elasticsearch.service.EsUtilService;
import com.igot.demand.entity.DemandEntity;
import com.networknt.schema.JsonSchema;
import com.networknt.schema.JsonSchemaFactory;
import com.networknt.schema.ValidationMessage;
import com.igot.demand.constants.Constants;
import com.igot.demand.exception.DemandCustomException;
import com.igot.demand.repository.DemandRepository;
import com.igot.demand.service.DemandService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.HttpStatus;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.InputStream;
import java.sql.Timestamp;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

@Service
@Slf4j
public class DemandServiceImpl implements DemandService {

    @Autowired
    private EsUtilService esUtilService;

    @Autowired
    private DemandRepository demandRepository;
    @Autowired
    private CacheService cacheService;
    @Autowired
    private ObjectMapper objectMapper;

    @Override
    public CustomResponse createDemand(JsonNode demandDetails) {
        CustomResponse response = new CustomResponse();
        validatePayload(Constants.PAYLOAD_VALIDATION_FILE, demandDetails);
        try {
                log.info("creating Demand");
                String id = String.valueOf(UUID.randomUUID());
                ((ObjectNode) demandDetails).put(Constants.IS_ACTIVE, Constants.ACTIVE_STATUS);
                Timestamp currentTime = new Timestamp(System.currentTimeMillis());
                ((ObjectNode) demandDetails).put(Constants.CREATED_DATE, String.valueOf(currentTime));
                ((ObjectNode) demandDetails).put(Constants.LAST_UPDATED_DATE, String.valueOf(currentTime));

                DemandEntity jsonNodeEntity = new DemandEntity();
                jsonNodeEntity.setId(id);
                jsonNodeEntity.setData(demandDetails);
                jsonNodeEntity.setCreatedOn(currentTime);
                jsonNodeEntity.setUpdatedOn(currentTime);

                DemandEntity saveJsonEntity = demandRepository.save(jsonNodeEntity);

                ObjectMapper objectMapper = new ObjectMapper();
                ObjectNode jsonNode = objectMapper.createObjectNode();
                jsonNode.set(Constants.ID, new TextNode(saveJsonEntity.getId()));
                jsonNode.setAll((ObjectNode) saveJsonEntity.getData());

                Map<String, Object> map = objectMapper.convertValue(jsonNode, Map.class);
                esUtilService.addDocument(Constants.INDEX_NAME, "_doc", id, map);

                cacheService.putCache(jsonNodeEntity.getId(), jsonNode);
                log.info("entity created");
                response.setMassage("Successfully created");
            response.setResponseCode(org.springframework.http.HttpStatus.valueOf(HttpStatus.SC_OK));
            return response;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }


    @Override
    public CustomResponse readDemand(String id) {
        log.info("reading demands for content");
        CustomResponse response = new CustomResponse();
        if (StringUtils.isEmpty(id)) {
            response.setResponseCode(org.springframework.http.HttpStatus.valueOf(HttpStatus.SC_INTERNAL_SERVER_ERROR));
            response.setMassage("Id not found");
            return response;
        }
        try {
            String cachedJson = cacheService.getCache(id);
            if (StringUtils.isNotEmpty(cachedJson)) {
                log.info("Record coming from redis cache");
                response
                        .getResult()
                        .put(Constants.RESULT, objectMapper.readValue(cachedJson, new TypeReference<>() {
                        }));
            } else {
                Optional<DemandEntity> entityOptional = demandRepository.findById(id);
                if (entityOptional.isPresent()) {
                    DemandEntity demandEntity = entityOptional.get();
                    cacheService.putCache(id, demandEntity.getData());
                    log.info("Record coming from postgres db");
                    response
                            .getResult()
                            .put(Constants.RESULT,
                                    objectMapper.convertValue(
                                            demandEntity.getData(), new TypeReference<>() {
                                            }));
                } else {
                    response.setResponseCode(org.springframework.http.HttpStatus.BAD_REQUEST);
                }
            }
        } catch (JsonMappingException e) {
            throw new DemandCustomException(Constants.ERROR, "error while processing", HttpStatus.SC_INTERNAL_SERVER_ERROR);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
        return response;
    }

    @Override
    public CustomResponse searchDemand(SearchCriteria searchCriteria) {
        String searchString = searchCriteria.getSearchString();
        CustomResponse response = new CustomResponse();
        if (searchString != null && searchString.length() < 2) {
            createErrorResponse(
                    response,
                    "Minimum 3 characters are required to search",
                    org.springframework.http.HttpStatus.BAD_REQUEST,
                    Constants.FAILED_CONST);
            return response;
        }
        try {
            SearchResult searchResult =
                    esUtilService.searchDocuments(Constants.INDEX_NAME, searchCriteria);
            response.getResult().put(Constants.RESULT, searchResult);
            createSuccessResponse(response);
            return response;
        } catch (Exception e) {
            createErrorResponse(
                    response, e.getMessage(), org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR, Constants.FAILED_CONST);
            return response;
        }
    }

    @Override
    public String delete(String id) {
        try {
            if (StringUtils.isNotEmpty(id)) {
                Optional<DemandEntity> entityOptional = demandRepository.findById(id);
                if (entityOptional.isPresent()) {
                    DemandEntity josnEntity = entityOptional.get();
                    JsonNode data = josnEntity.getData();
                    Timestamp currentTime = new Timestamp(System.currentTimeMillis());
                    if(data.get(Constants.IS_ACTIVE).asBoolean()){
                        ((ObjectNode) data).put("isActive", false);
                        josnEntity.setData(data);
                        josnEntity.setId(id);
                        josnEntity.setUpdatedOn(currentTime);
                        DemandEntity updateJsonEntity = demandRepository.save(josnEntity);
                        Map<String, Object> map = objectMapper.convertValue(data, Map.class);
                        esUtilService.addDocument(Constants.INDEX_NAME, "_doc", id, map);
                        cacheService.putCache(id,data);
                        return "Entity details deleted successfully.";
                    }else
                        return "Entity is already inactive.";
                }else return "Entity not found.";
            } else return "Invalid entity ID.";
        } catch (Exception e) {
            return "Error deleting Entity with ID " + id + " " + e.getMessage();
        }
    }

    public void validatePayload(String fileName, JsonNode payload) {
        try {
            JsonSchemaFactory schemaFactory = JsonSchemaFactory.getInstance();
            InputStream schemaStream = schemaFactory.getClass().getResourceAsStream(fileName);
            JsonSchema schema = schemaFactory.getSchema(schemaStream);

            Set<ValidationMessage> validationMessages = schema.validate(payload);
            if (!validationMessages.isEmpty()) {
                StringBuilder errorMessage = new StringBuilder("Validation error(s): \n");
                for (ValidationMessage message : validationMessages) {
                    errorMessage.append(message.getMessage()).append("\n");
                }
                throw new DemandCustomException(Constants.ERROR, errorMessage.toString());
            }
        } catch (DemandCustomException e) {
            throw new DemandCustomException(Constants.ERROR, "Failed to validate payload: " + e.getMessage());
        }
    }

    public void createSuccessResponse(CustomResponse response) {
        response.setParams(new RespParam());
        response.getParams().setStatus("SUCCESS");
        response.setResponseCode(org.springframework.http.HttpStatus.OK);
    }

    public void createErrorResponse(
            CustomResponse response, String errorMessage, org.springframework.http.HttpStatus httpStatus, String status) {
        response.setParams(new RespParam());
        //response.getParams().setErrorMsg(errorMessage);
        response.getParams().setStatus(status);
        response.setResponseCode(httpStatus);
    }
}
