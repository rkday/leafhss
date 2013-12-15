# LeafHSS

A modern open-source HSS for use in IMS networks.

The current version is available for download [here](https://s3.amazonaws.com/org.leafhss/org.leafhss.hss-0.1.0-SNAPSHOT-standalone.jar) and can be run with `java -jar org.leafhss.hss-0.1.0-SNAPSHOT-standalone.jar [options]`

## Usage

LeafHSS has two modes - a full mode (where subscriber data is provisioned and stored in a database) and an auto-answer mode (where all requests receive default responses) which is useful for testing other IMS components. The auto-answer mode currently works, and the full mode is still in development.

To start LeafHSS in auto-answer mode, use the command-line option `--autoanswer`. Four other command-line options control the responses:

- `--scscf-sip-uri` sets the S-CSCF with which all subscribers are considered registered
- `--visited-network-id` sets the access network which all subscribers are allowed to register from
- `--digest-realm` sets the realm used in SIP Digest authentication
- `--standard-password` sets the password used for all subscribers

In this mode:

- User-Authorization-Requests succeed if the Visited-Network matches the --visited-network-id parameter and the Public-Identity is the SIP URI variant of the User-Name (e.g "sip:1234@example.com" and "1234@example.com"). The User-Authorization-Answer contains the Server-Name set by the `--scscf-sip-uri` parameter
- Multimedia-Auth-Requests succeed if the Public-Identity is the SIP URI variant of the User-Name (e.g "sip:1234@example.com" and "1234@example.com") and the authentication type requested is "SIP Digest" or "Unknown". The answer always contains the realm specified by the `--digest-realm` parameter and a HA1 created by a hash of the User-Name, the realm and the password specified by the `--standard-password` parameter.
- Server-Assignment-Requests succeed if the Public-Identity is the SIP URI variant of the User-Name (e.g "sip:1234@example.com" and "1234@example.com"). The User-Data in the answer contains no initial filter criteria.
- Location-Information-Requests always succeed. The answer contains the Server-Name set by the `--scscf-sip-uri` parameter.

## Diameter

LeafHSS uses the Mobicents jDiameter stack, and so Diameter-level configuration (e.g. peers allowed to connect) must be specified in a Jdiameter config file. The `--jdiameter-config` command line option allows you to specify which config file to use; an example file is [available here](https://raw.github.com/rkday/leafhss/master/resources/config.xml), and [Red Hat have a guide to the configuration here](https://access.redhat.com/site/documentation/en-US/JBoss_Communications_Platform/5.1/html/Diameter_User_Guide/jdiameter-configuration.html).

## License

Copyright © 2013 Robert Day

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
