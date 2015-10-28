package org.wso2.carbon.apimgt.rest.api.store.impl;

import org.apache.commons.lang.StringUtils;
import org.wso2.carbon.apimgt.api.APIConsumer;
import org.wso2.carbon.apimgt.api.APIManagementException;
import org.wso2.carbon.apimgt.api.model.APIIdentifier;
import org.wso2.carbon.apimgt.api.model.Application;
import org.wso2.carbon.apimgt.api.model.SubscribedAPI;
import org.wso2.carbon.apimgt.api.model.Subscriber;
import org.wso2.carbon.apimgt.rest.api.store.SubscriptionsApiService;
import org.wso2.carbon.apimgt.rest.api.store.dto.SubscriptionDTO;
import org.wso2.carbon.apimgt.rest.api.store.exception.InternalServerErrorException;
import org.wso2.carbon.apimgt.rest.api.store.exception.NotFoundException;
import org.wso2.carbon.apimgt.rest.api.store.utils.mappings.APIMappingUtil;
import org.wso2.carbon.apimgt.rest.api.store.utils.mappings.SubscriptionMappingUtil;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import javax.ws.rs.core.Response;
import org.wso2.carbon.apimgt.rest.api.util.utils.RestApiUtil;


public class SubscriptionsApiServiceImpl extends SubscriptionsApiService {
    @Override
    public Response subscriptionsGet(String apiId, String applicationId, String groupId, String accept,
            String ifNoneMatch) {

        String username = RestApiUtil.getLoggedInUsername();
        Subscriber subscriber = new Subscriber(username);
        Set<SubscribedAPI> subscriptions = new HashSet<>();
        try {
            APIConsumer apiConsumer = RestApiUtil.getConsumer(username);
            if (!StringUtils.isEmpty(apiId)) {
                APIIdentifier apiIdentifier = APIMappingUtil.getAPIIdentifier(apiId);
                subscriptions = apiConsumer.getSubscribedIdentifiers(subscriber, apiIdentifier, groupId);

            } else if (!StringUtils.isEmpty(applicationId)) {
                Application application = apiConsumer.getApplicationById(Integer.parseInt(applicationId));
                subscriptions =
                        apiConsumer.getSubscribedAPIs(subscriber, application.getName(), application.getGroupId());
            }

            List<SubscriptionDTO> subscriptionDTOs = new ArrayList<>();
            for (SubscribedAPI subscription : subscriptions) {
                SubscriptionDTO subscriptionDTO = SubscriptionMappingUtil.fromSubscriptiontoDTO(subscription);
                //when retrieving subscriptions from getSubscribedAPIs(), existing application ids are not populated
                if (!StringUtils.isEmpty(applicationId)) {
                    subscriptionDTO.setApplicationId(applicationId);
                }
                subscriptionDTOs.add(subscriptionDTO);
            }
            return Response.ok().entity(subscriptionDTOs).build();

        } catch (APIManagementException e) {
            throw new InternalServerErrorException(e);
        }
    }

    @Override
    public Response subscriptionsPost(SubscriptionDTO body, String contentType) {
        String username = RestApiUtil.getLoggedInUsername();
        APIConsumer apiConsumer = null;
        try {
            //todo: Validation for allowed throttling tiers and Tenant based validation for subscription
            apiConsumer = RestApiUtil.getConsumer(username);
            String apiId = body.getApiId();
            String applicationId = body.getApplicationId();
            APIIdentifier apiIdentifier = APIMappingUtil.getAPIIdentifier(apiId);
            apiIdentifier.setTier(body.getTier());
            int subscriptionId =
                    apiConsumer.addSubscription(apiIdentifier, username, Integer.parseInt(applicationId));
            SubscribedAPI addedSubscribedAPI = apiConsumer.getSubscriptionById(subscriptionId);
            SubscriptionDTO addedSubscriptionDTO = SubscriptionMappingUtil.fromSubscriptiontoDTO(addedSubscribedAPI);
            //todo: use a proper way other than using "subscriptions/"
            return Response.created(new URI("subscriptions/" + subscriptionId)).entity(addedSubscriptionDTO).build();
        } catch (APIManagementException | URISyntaxException e) {
            throw new InternalServerErrorException(e);
        }
    }
    @Override
    public Response subscriptionsSubscriptionIdGet(String subscriptionId, String accept, String ifNoneMatch,
            String ifModifiedSince) {
        String username = RestApiUtil.getLoggedInUsername();
        APIConsumer apiConsumer = null;
        try {
            apiConsumer = RestApiUtil.getConsumer(username);
            SubscribedAPI subscribedAPI = apiConsumer.getSubscriptionById(Integer.parseInt(subscriptionId));
            if (subscribedAPI != null) {
                SubscriptionDTO subscriptionDTO = SubscriptionMappingUtil.fromSubscriptiontoDTO(subscribedAPI);
                return Response.ok().entity(subscriptionDTO).build();
            } else {
                throw new NotFoundException();
            }
        } catch (APIManagementException e) {
            throw new InternalServerErrorException(e);
        }
    }

    @Override
    public Response subscriptionsSubscriptionIdDelete(String subscriptionId, String ifMatch, String ifUnmodifiedSince) {
        String username = RestApiUtil.getLoggedInUsername();
        APIConsumer apiConsumer = null;
        try {
            apiConsumer = RestApiUtil.getConsumer(username);
           // apiConsumer.removeSubscriptionById(Integer.parseInt(subscriptionId));
            return Response.ok().build();
        } catch (APIManagementException e) {
            throw new InternalServerErrorException(e);
        }
    }
}