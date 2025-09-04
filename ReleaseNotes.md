<img align="right" width="200" height="37" src="media/Gematik_Logo_Flag.png"/> <br/> 
 
# Release notes

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
