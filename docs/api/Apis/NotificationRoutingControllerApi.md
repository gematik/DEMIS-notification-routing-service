# NotificationRoutingControllerApi

All URIs are relative to *http://localhost*

| Method | HTTP request | Description |
|------------- | ------------- | -------------|
| [**determineRouting**](NotificationRoutingControllerApi.md#determineRouting) | **POST** /routing |  |
| [**determineRuleBasedRouting**](NotificationRoutingControllerApi.md#determineRuleBasedRouting) | **POST** /routing/v2 |  |
| [**findHealthOfficeByAddress**](NotificationRoutingControllerApi.md#findHealthOfficeByAddress) | **GET** /routing/health-office |  |


<a name="determineRouting"></a>
# **determineRouting**
> RoutingOutput determineRouting(body)



### Parameters

|Name | Type | Description  | Notes |
|------------- | ------------- | ------------- | -------------|
| **body** | **String**|  | |

### Return type

[**RoutingOutput**](../Models/RoutingOutput.md)

### Authorization

No authorization required

### HTTP request headers

- **Content-Type**: application/json
- **Accept**: application/json

<a name="determineRuleBasedRouting"></a>
# **determineRuleBasedRouting**
> Object determineRuleBasedRouting(isTestUser, testUserID, body)



### Parameters

|Name | Type | Description  | Notes |
|------------- | ------------- | ------------- | -------------|
| **isTestUser** | **Boolean**|  | [default to null] |
| **testUserID** | **String**|  | [default to null] |
| **body** | **String**|  | |

### Return type

**Object**

### Authorization

No authorization required

### HTTP request headers

- **Content-Type**: application/json
- **Accept**: application/json

<a name="findHealthOfficeByAddress"></a>
# **findHealthOfficeByAddress**
> String findHealthOfficeByAddress(street, no, postalCode, city, countryCode)



### Parameters

|Name | Type | Description  | Notes |
|------------- | ------------- | ------------- | -------------|
| **street** | **String**|  | [optional] [default to null] |
| **no** | **String**|  | [optional] [default to null] |
| **postalCode** | **String**|  | [optional] [default to null] |
| **city** | **String**|  | [optional] [default to null] |
| **countryCode** | **String**|  | [optional] [default to 20422] |

### Return type

**String**

### Authorization

No authorization required

### HTTP request headers

- **Content-Type**: Not defined
- **Accept**: */*

