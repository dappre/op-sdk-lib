# op-sdk-lib

## What is this?
Let's start by taking it's name apart (last to first)
- lib: This is a library to be used in other (java/JVM) project
- sdk: It's a Software Development Kit, i.e.: helping you to build your software
- OP: The software you'll be building with this is an Openid connect Provider. More specifically we're aiming at http://openid.net/specs/openid-connect-core-1_0.html

## How do I use it?
- Include it as a Maven dependency (not yet(?) available publicly, so check the whole thing out and build locally)
- nl.qiy.oic.op.api.AuthenticationResource is a JAX-RS endpoint where authorization requests will come in, so make sure that is known to your JAX-RS framework (e.g. Jersey, RestEasy)
- Extension points are implemented in a ServiceLoader pattern, the interfaces to implement can be found in nl.qiy.oic.op.service.spi. Examples can be found in [op-sdk-spi-impl]


