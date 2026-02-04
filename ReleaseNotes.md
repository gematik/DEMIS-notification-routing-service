<div style="text-align:right"><img src="https://raw.githubusercontent.com/gematik/gematik.github.io/master/Gematik_Logo_Flag_With_Background.png" width="250" height="47" alt="gematik GmbH Logo"/> <br/> </div> <br/>

# Release notes

## 2.5.1
- removed feature flag FEATURE_FLAG_TUBERCULOSIS_ROUTING_ENABLED
- removed legacy service for routing


## 2.5.0
- upgraded spring parent to version 2.14.17
- added workaround for cologne tbc routing
- added action PSEUDO_COPY for $7.3 excerpts to routingConfig_73enabled.json
- added routing results for anonymous follow up to routingConfig_73enabled.json
- removed routing results for §7.3 from routingConfig_with_follow_up
- added FEATURE_FLAG_EXCERPT_ENCRYPTION_ENABLED
- added action ENCRYPT for §6.1 and §7.1 excerpts to routingConfig_73enabled_excerptEncryption.json

## 2.4.3
- upgraded spring parent to version 2.14.11
- increased standard memory resources to 900Mi in helm chart

## 2.4.2
- fix notificationId handling for follow up notification disease

## 2.4.1
- add validation of notificationId to be UUID, before sending request to get responsible health office to DLS, return with 422 if invalid
- bump spring parent to 2.14.2

## 2.4.0
- add client to lookup destination notification reader information for §7.1 follow up notifications
- updated SpringParent to 2.13.2
- remove store_destination action
- drop notification category parameter from DLS request
- add missing allowedRoles for tuberculosis routing in follow-up

## 2.3.6
- refine rules for invalid 7.1 bundles
- implement option to return allowed roles for routing result
- implement option to return custodian field for routing result
- add support for new FUTS API endpoints

## 2.3.5
- Tuberculosis routing

## 2.3.4
- add default feature flags FEATURE_FLAG_NOTIFICATIONS_7_3, FEATURE_FLAG_FOLLOW_UP_NOTIFICATIONS to values.yaml

## 2.3.3
- additional results for follow_up notifications 7.1/6.1

## 2.3.2
- Removed already active feature flags
- Updated dependencies

## 2.3.1
- Hotfix for matching Bundles as 7.4 that are 7.1

## 2.3.0
- Preview for fuzzy search algorithm to lookup health offices

## 2.2.3
- Fixed minor issues when faulty FHIR requests are received
- Updated dependencies

## 2.2.2
- Updated OSPO-resources for adding additional notes and disclaimer
- setting new resources in helm chart
- setting new timeouts and retries in helm chart
- updating dependencies

## 2.2.1
- Add service API documentation 
- Splitting §7.3 notifications into anonymous and regular

## 2.2.0
- Add support for §7.3 notifications (optional bundle actions)

## 2.1.0
### changed
- First official GitHub-Release
- Update Base-Image to OSADL
- Dependency-Updates (CVEs et al.)
- Implement rule-engine

## 1.0.0
### added
- SpringBoot 3.0.1

### changed
- License updated to 2023

### fixed
- SonarQube Code Smells

### security
- Removed SnakeYAML (OWASP Scan)
