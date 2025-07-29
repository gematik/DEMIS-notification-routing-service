<img src="media/Gematik_Logo_Flag.png" alt="Gematik Logo" width="200" height="37">

[![Quality Gate Status](https://sonar.prod.ccs.gematik.solutions/api/project_badges/measure?project=de.gematik.demis%3Anotification-routing-service&metric=alert_status&token=sqb_1d5c90204d1303826a75a29a8ed57699fc2da1b5)](https://sonar.prod.ccs.gematik.solutions/dashboard?id=de.gematik.demis%3Anotification-routing-service)
[![Vulnerabilities](https://sonar.prod.ccs.gematik.solutions/api/project_badges/measure?project=de.gematik.demis%3Anotification-routing-service&metric=vulnerabilities&token=sqb_1d5c90204d1303826a75a29a8ed57699fc2da1b5)](https://sonar.prod.ccs.gematik.solutions/dashboard?id=de.gematik.demis%3Anotification-routing-service)
[![Bugs](https://sonar.prod.ccs.gematik.solutions/api/project_badges/measure?project=de.gematik.demis%3Anotification-routing-service&metric=bugs&token=sqb_1d5c90204d1303826a75a29a8ed57699fc2da1b5)](https://sonar.prod.ccs.gematik.solutions/dashboard?id=de.gematik.demis%3Anotification-routing-service)
[![Code Smells](https://sonar.prod.ccs.gematik.solutions/api/project_badges/measure?project=de.gematik.demis%3Anotification-routing-service&metric=code_smells&token=sqb_1d5c90204d1303826a75a29a8ed57699fc2da1b5)](https://sonar.prod.ccs.gematik.solutions/dashboard?id=de.gematik.demis%3Anotification-routing-service)
[![Technical Debt](https://sonar.prod.ccs.gematik.solutions/api/project_badges/measure?project=de.gematik.demis%3Anotification-routing-service&metric=sqale_index&token=sqb_1d5c90204d1303826a75a29a8ed57699fc2da1b5)](https://sonar.prod.ccs.gematik.solutions/dashboard?id=de.gematik.demis%3Anotification-routing-service)
[![Duplicated Lines (%)](https://sonar.prod.ccs.gematik.solutions/api/project_badges/measure?project=de.gematik.demis%3Anotification-routing-service&metric=duplicated_lines_density&token=sqb_1d5c90204d1303826a75a29a8ed57699fc2da1b5)](https://sonar.prod.ccs.gematik.solutions/dashboard?id=de.gematik.demis%3Anotification-routing-service)
[![Lines of Code](https://sonar.prod.ccs.gematik.solutions/api/project_badges/measure?project=de.gematik.demis%3Anotification-routing-service&metric=ncloc&token=sqb_1d5c90204d1303826a75a29a8ed57699fc2da1b5)](https://sonar.prod.ccs.gematik.solutions/dashboard?id=de.gematik.demis%3Anotification-routing-service)
[![Coverage](https://sonar.prod.ccs.gematik.solutions/api/project_badges/measure?project=de.gematik.demis%3Anotification-routing-service&metric=coverage&token=sqb_1d5c90204d1303826a75a29a8ed57699fc2da1b5)](https://sonar.prod.ccs.gematik.solutions/dashboard?id=de.gematik.demis%3Anotification-routing-service)

# Notification-Routing-Service

<details>
  <summary>Table of Contents</summary>
  <ol>
    <li>
      <a href="#about-the-project">About The Project</a>
      <ul>
        <li><a href="#release-notes">Release Notes</a></li>
      </ul>
    </li>
    <li>
      <a href="#getting-started">Getting Started</a>
      <ul>
        <li><a href="#prerequisites">Prerequisites</a></li>
        <li><a href="#routing-data">Routing Data</a></li>
        <li><a href="#installation">Installation</a></li>
      </ul>
    </li>
    <li><a href="#usage">Usage</a></li>
    <li><a href="#security-policy">Security Policy</a></li>
    <li><a href="#contributing">Contributing</a></li>
    <li><a href="#license">License</a></li>
    <li><a href="#contact">Contact</a></li>
  </ol>
</details>

## About The Project
This project contains the DEMIS microservice "Notification Routing Service". The task of the service is to provide the 
destination of a notification. The evaluation is based on the assignment of all addresses in Germany to a public health 
department by the RKI. In principle, the IfSG is followed here, which prescribes the order in which the addresses of a 
notification must be used to determine the recipient of a notification. Any special cases, such as federal state-specific 
peculiarities relating to specific pathogens, are dealt with specifically.

### Release Notes

See [ReleaseNotes](ReleaseNotes.md) for all information regarding the (newest) releases.

## Getting Started

### Prerequisites

The Project requires Java 21 and Maven 3.8+.

### Routing Data

Routing Data (lookup csv files) are contained in a separat repository.
This image is maintained in DockerHub: [demis-notification-routing-data](https://hub.docker.com/repository/docker/gematik1/demis-notification-routing-data/general).

For local testing you can check out the GitHub repository [demis-notification-routing-data](https://github.com/gematik/DEMIS-notification-routing-data)
and set lookup-data-directory in die application.properties (or the environment-variable DATA_DIR_LOOKUP) to the path 
of the checked out repository.

Note: For just building a docker image this step is not required.

### Installation

The Project can be built with the following command:

```sh
mvn clean install
```

The Docker Image associated to the service can be built with the extra profile `docker`:

```sh
mvn clean install -Pdocker
```

## Usage

The application can be executed from a JAR file or a Docker Image:

```sh
# As JAR Application
java -jar target/notification-routing-service.jar
# As Docker Image
docker run --rm -it -p 8080:8080 notification-routing-service:latest
```

It can also be deployed on Kubernetes by using the Helm Chart defined in the folder `deployment/helm/notification-routing-service`:

```ssh
helm install notification-routing-service ./deployment/helm/notification-routing-service
```

### Endpoints

| Endpoint                | Method | Input                                                                                                                                                                                                                                                                                                                                                                                   | Output                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                          |
|-------------------------|--------|-----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|-----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| /routing/               | POST   | serialisierte vollständige FHIR-Meldungen                                                                                                                                                                                                                                                                                                                                               | Routing-Ziele für eine FHIR-Meldung Bsp. <pre>{<br/>  "healthOffices": {<br/>    "NOTIFIED_PERSON_CURRENT": "1.05.3.78.",<br/>    "NOTIFIED_PERSON_ORDINARY": "1.11.0.11.01.",<br/>    "NOTIFIED_PERSON_PRIMARY": "1.05.9.11.",</br>    "SUBMITTER": "1.11.0.11.01.",<br/>    "NOTIFIER": "1.11.0.11.01."<br/>  }<br/>  "responsible": "1.05.3.78."<br/>}</pre>                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                 |
| /routing/health-office/ | GET    | optional QueryParams:<ul><li>line (This component contains the house number, apartment number, street name, street direction, P.O. Box number, delivery hints, and similar address information.)</li><li>postalCode</li><li>city</li><li>district (The name of the administrative area)</li><li>state (Sub-unit of country)</li><li>country (a nation as commonly understood)</li></ul> | Identifikator des Gesundheitsamtes als Zeichenkette Bsp. `1.11.0.09.01.`                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                        |
| /routing/v2             | POST   | serialisierte vollständige FHIR-Meldungen                                                                                                                                                                                                                                                                                                                                               | Routing-Ziele für eine FHIR-Meldung Bsp. <pre>{<br/>  "type": "laboratory",<br/>  "notificationCategory": "7.1",<br/>  "routes": [<br/>    {<br/>      "type": "specific_receiver",<br/>      "specific_receiverId": "1.",<br/>      "modifications": [<br/>        "create_pseudo_copy"<br/>      ]<br/>    },<br/>    {<br/>      "type": "responsible_health_office",<br/>      "specific_receiverId": "2.05.3.78.",<br/>      "modifications": [<br/>        "encrypt"<br/>      ]<br/>    },<br/>    {<br/>      "type": "responsible_health_office_sormas",<br/>      "specific_receiverId": "2.05.3.78.",<br/>      "modifications": [<br/>        "encrypt"<br/>      ]<br/>    }<br/>  ],<br/>  "healthOffices": {<br/>    "NOTIFIED_PERSON_CURRENT": "1.05.3.78.",<br/>    "NOTIFIED_PERSON_ORDINARY": "1.11.0.11.01.",<br/>    "NOTIFIED_PERSON_PRIMARY": "1.05.9.11.",<br/>    "SUBMITTER": "1.11.0.11.01.",<br/>    "NOTIFIER": "1.11.0.11.01."<br/>  },<br/>  "responsible": "1.05.3.78."<br/>}</pre> |

### Feature Flags

The Spring application properties of the service.

| Flag                                      | Description                                                                                 | Default |
|-------------------------------------------|---------------------------------------------------------------------------------------------|---------|
| feature.flag.notifications.7_3            | Set to true to return routing information suitable for 7.3 processing                       | false   |
| feature.flag.search.fuzzy                 | Set to true to return lookup data from new fuzzy search algorithm                           | false   |
| feature.flag.search.comparison            | Set to true to compare new fuzzy search algorithm to old algorithm (will run new algorithm) | false   |
| feature.flag.tuberculosis.routing.enabled | Set to true to route health office to tuberculosis health offices (if possible)             | false   |


## Security Policy
If you want to see the security policy, please check our [SECURITY.md](.github/SECURITY.md).

## Contributing
If you want to contribute, please check our [CONTRIBUTING.md](.github/CONTRIBUTING.md).

## License

Copyright 2024-2025 gematik GmbH

EUROPEAN UNION PUBLIC LICENCE v. 1.2

EUPL © the European Union 2007, 2016

See the [LICENSE](./LICENSE.md) for the specific language governing permissions and limitations under the License

## Additional Notes and Disclaimer from gematik GmbH

1. Copyright notice: Each published work result is accompanied by an explicit statement of the license conditions for use. These are regularly typical conditions in connection with open source or free software. Programs described/provided/linked here are free software, unless otherwise stated.
2. Permission notice: Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation files (the "Software"), to deal in the Software without restriction, including without limitation the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is furnished to do so, subject to the following conditions:
    1. The copyright notice (Item 1) and the permission notice (Item 2) shall be included in all copies or substantial portions of the Software.
    2. The software is provided "as is" without warranty of any kind, either express or implied, including, but not limited to, the warranties of fitness for a particular purpose, merchantability, and/or non-infringement. The authors or copyright holders shall not be liable in any manner whatsoever for any damages or other claims arising from, out of or in connection with the software or the use or other dealings with the software, whether in an action of contract, tort, or otherwise.
    3. We take open source license compliance very seriously. We are always striving to achieve compliance at all times and to improve our processes. If you find any issues or have any suggestions or comments, or if you see any other ways in which we can improve, please reach out to: ospo@gematik.de
3. Please note: Parts of this code may have been generated using AI-supported technology. Please take this into account, especially when troubleshooting, for security analyses and possible adjustments.

## Contact
E-Mail to [DEMIS Entwicklung](mailto:demis-entwicklung@gematik.de?subject=[GitHub]%20Notification-Routing-Service)
