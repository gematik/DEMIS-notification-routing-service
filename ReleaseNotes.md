<div style="text-align:right"><img src="https://raw.githubusercontent.com/gematik/gematik.github.io/master/Gematik_Logo_Flag_With_Background.png" width="250" height="47" alt="gematik GmbH Logo"/> <br/> </div> <br/>

# Release notes

## Release 2.5.0
- upgraded spring parent to version 2.14.12
- add workaround for cologne tbc routing

## Release 2.4.3
- upgraded spring parent to version 2.14.11
- increased standard memory resources to 900Mi in helm chart

## Release 2.4.2
- fix notificationId handling for follow up notification disease

## Release 2.4.1
- add validation of notificationId to be UUID, before sending request to get responsible health office to DLS, return with 422 if invalid
- bump spring parent to 2.14.2

## Release 2.4.0
- add client to lookup destination notification reader information for §7.1 follow up notifications
- updated SpringParent to 2.13.2
- remove store_destination action
- drop notification category parameter from DLS request
- add missing allowedRoles for tuberculosis routing in follow-up

## Release 2.3.6
- refine rules for invalid 7.1 bundles
- implement option to return allowed roles for routing result
- implement option to return custodian field for routing result
- add support for new FUTS API endpoints

## Release 2.3.5
- Tuberculosis routing

## Release 2.3.4
- add default feature flags FEATURE_FLAG_NOTIFICATIONS_7_3, FEATURE_FLAG_FOLLOW_UP_NOTIFICATIONS to values.yaml

## Release 2.3.3
- additional results for follow_up notifications 7.1/6.1

## Release 2.3.2
- Removed already active feature flags
- Updated dependencies

## Release 2.3.1
- Hotfix for matching Bundles as 7.4 that are 7.1

## Release 2.3.0
- Preview for fuzzy search algorithm to lookup health offices

## Release 2.2.3
- Fixed minor issues when faulty FHIR requests are received
- Updated dependencies

## Release 2.2.2
- Updated OSPO-resources for adding additional notes and disclaimer
- setting new resources in helm chart
- setting new timeouts and retries in helm chart
- updating dependencies

## Release 2.2.1
- Add service API documentation 
- Splitting §7.3 notifications into anonymous and regular

## Release 2.2.0
- Add support for §7.3 notifications (optional bundle actions)

## Release 2.1.0
### changed
- First official GitHub-Release
- Update Base-Image to OSADL
- Dependency-Updates (CVEs et al.)
- Implement rule-engine

## Release 1.0.0
### added
- SpringBoot 3.0.1

### changed
- License updated to 2023

### fixed
- SonarQube Code Smells

### security
- Removed SnakeYAML (OWASP Scan)
