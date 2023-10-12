# Obelisk Signing Portal Client application

Client application for signing electronic documents with a certificate stored on a smartcard, token or local keystore in cooperation with the OBELISK Signing Portal web application.
The client supports guaranteed or qualified signatures.

https://www.sefira.cz/en/obelisk-signing-portal-electronic-signature/


Licensed as EUPL-1.2

Forked and heavily modified from Nowina's NexU signing application 
https://github.com/nowina-solutions/nexu

## Features

- Support of qualified devices eObčanka, Starcos, SafeNet, TokenME, etc.
- Support for PKCS#11 and MSCAPI interfaces
- Keystore support for PFX, JKS, JCEKS formats
- Compatible with Windows, Mac and Linux
- Ability to configure temporary QPIN caching (qualified signature PIN code)
- Application is intended to be controlled by handling custom URI, therefore the app doesn't need to run a local server or open any ports and can run in multi-seat environment.