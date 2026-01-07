# Changelog

## [1.7.0](https://github.com/hks2002/docs-server-vertx/compare/v1.6.0...v1.7.0) (2026-01-07)


### Features

* ✨add access log ([9fa93f1](https://github.com/hks2002/docs-server-vertx/commit/9fa93f18faa83378d8ad1b40d3d95eb53026a635))
* ✨add obfuscate file name ([bc007be](https://github.com/hks2002/docs-server-vertx/commit/bc007be50c469c551fd6be696658ae5250d775db))
* ✨add user access log api ([3c2d7c5](https://github.com/hks2002/docs-server-vertx/commit/3c2d7c5cf13c360495578ccdd35b07afc99a3ade))
* ✨better disposition file name ([64c81de](https://github.com/hks2002/docs-server-vertx/commit/64c81de49daf883f6a3eb18eb151ea5cfde73eb7))
* ✨better history sql ([f45bbca](https://github.com/hks2002/docs-server-vertx/commit/f45bbca86f7a81b18f0decbbb712846dd9790428))
* ✨better route log ([8d14284](https://github.com/hks2002/docs-server-vertx/commit/8d14284b4d9ac35aa145cd5eb21cf01fae80411d))
* ✨better startup, and bug fix ([7dd575a](https://github.com/hks2002/docs-server-vertx/commit/7dd575ac38c902b5cb4e184c00245a09503f19d5))
* ✨config file for jar deploy ([337a087](https://github.com/hks2002/docs-server-vertx/commit/337a0874481649ce979fc446e4fce12bcd525c07))
* ✨different log4j2 config file for dev and prod ([c7c5a21](https://github.com/hks2002/docs-server-vertx/commit/c7c5a21e83622b96f43126eab97b51988245b8a5))
* ✨fat and thin jar supported ([4424a25](https://github.com/hks2002/docs-server-vertx/commit/4424a25d933b061ee3c7e6edf3cee77b0d1cd878))
* ✨message for dms processing to web front ([a2c2df4](https://github.com/hks2002/docs-server-vertx/commit/a2c2df407e1e367c36a61e62a7249214a44aec09))
* ✨new DMS/DOCS service ([ca4755b](https://github.com/hks2002/docs-server-vertx/commit/ca4755b8a44790d4ded6cd9a4256de0526e92f41))
* ✨new httpService and DMSservice ([84de6ad](https://github.com/hks2002/docs-server-vertx/commit/84de6add38f538d013c3fc6f145dcb29e0eac789))
* ✨new UserService ([13655da](https://github.com/hks2002/docs-server-vertx/commit/13655daf97777293da9d5ad62031183f97b78947))
* ✨session active detect ([872511d](https://github.com/hks2002/docs-server-vertx/commit/872511dab6d0936caf6c172642b21d822468cbe0))
* ✨ssl ad login support ([53f5260](https://github.com/hks2002/docs-server-vertx/commit/53f526001f8ffa5968af366da01d68cec6e79259))
* ✨update java21 ([a352f20](https://github.com/hks2002/docs-server-vertx/commit/a352f20f324c13aaa53955ae1f34118705092092))
* ✨xml parse for dms service ([d2b7830](https://github.com/hks2002/docs-server-vertx/commit/d2b7830445cc8f6629da28d3e99e5fd080c195ba))


### Bug Fixes

* 🐛 escape character insert ([8463b90](https://github.com/hks2002/docs-server-vertx/commit/8463b9007a2e3ad4a71c636725126fc08ea3fdae))
* 🐛 fix empty dateString cause exception ([ea64619](https://github.com/hks2002/docs-server-vertx/commit/ea6461978291c2ab8c5a7c201d7aaa68aac09784))
* 🐛 fix long time no response ([083b1fd](https://github.com/hks2002/docs-server-vertx/commit/083b1fd97358e50222c9f9f18e2a5c88ce9b1085))
* 🐛 fix query from sqlserver with  "'" ([f82b3b9](https://github.com/hks2002/docs-server-vertx/commit/f82b3b9ab1eea46d5cf9a91910212ef5b184fe2b))
* 🐛add default mime type if unknown ([ec2cc96](https://github.com/hks2002/docs-server-vertx/commit/ec2cc9689ac38cd7cf747938fb84eac16e4cc59b))
* 🐛config and option for jar deploy ([53959d8](https://github.com/hks2002/docs-server-vertx/commit/53959d89d0a17789cff7c03d049fa073e76cfcb2))
* 🐛fix  disposition file name ([18ff5cf](https://github.com/hks2002/docs-server-vertx/commit/18ff5cf9f3529783c44db43079f11baaf0350505))
* 🐛fix  Thread blocked when download files ([f83a37f](https://github.com/hks2002/docs-server-vertx/commit/f83a37fe960701a43e64c3572896981f987d83c6))
* 🐛fix add water maker error response ([e44f2aa](https://github.com/hks2002/docs-server-vertx/commit/e44f2aa2ef722f31fc74c3b2e58823e2b2355066))
* 🐛fix Connection pool reached max ([c6c42fe](https://github.com/hks2002/docs-server-vertx/commit/c6c42feec610a13d6c60a9677d6bb3603d06aefd))
* 🐛fix db connection close ([f9daefd](https://github.com/hks2002/docs-server-vertx/commit/f9daefdac5ea9165ba720a0e489aab4a816716e7))
* 🐛fix db connection full ([acea13d](https://github.com/hks2002/docs-server-vertx/commit/acea13d982d11d8518807044ce57b8ee1e04f7d2))
* 🐛fix db connection issue ([9dc2d76](https://github.com/hks2002/docs-server-vertx/commit/9dc2d76bc80d2938673f2893b602ab4c2e182a74))
* 🐛fix dms download and upload bugs ([d345b93](https://github.com/hks2002/docs-server-vertx/commit/d345b9307477cd39e90340a5b2fd04fc7b04894a))
* 🐛fix dms server error ([814d9cc](https://github.com/hks2002/docs-server-vertx/commit/814d9cc17a76de69109522a863c4ab2cfd226c5a))
* 🐛fix extension bug ([848fd91](https://github.com/hks2002/docs-server-vertx/commit/848fd91204f1518c3d69c2dc73dc8a0841698749))
* 🐛fix history url bug ([adadb3d](https://github.com/hks2002/docs-server-vertx/commit/adadb3d265858bdca701d83e360c00c3fd647027))
* 🐛fix logout ([9db5dc6](https://github.com/hks2002/docs-server-vertx/commit/9db5dc68094632156ae8c0e67c0ef34d946bce9f))
* 🐛fix null of user ([144dc3e](https://github.com/hks2002/docs-server-vertx/commit/144dc3ee702c1a6d6d1789114ec19acd84ae35d1))
* 🐛fix server no response ([84fe272](https://github.com/hks2002/docs-server-vertx/commit/84fe2727ab121d2458f720d20467c349de91fd0c))
* 🐛fix static construct assign ([0219453](https://github.com/hks2002/docs-server-vertx/commit/0219453baff17968dcc0a67af55ee2a45553a9b8))
* 🐛fix thread blocked ([db56f5e](https://github.com/hks2002/docs-server-vertx/commit/db56f5eb234acd0bf2f0b2a02e117a6f2177c032))
* 🐛fix upload fail ([3206c04](https://github.com/hks2002/docs-server-vertx/commit/3206c04ee2786cb0b0c2bb08fe38d18209640ad4))
* 🐛new httpService and DMSService ([8ffd005](https://github.com/hks2002/docs-server-vertx/commit/8ffd005a1edf432257ebce505b87fe108b21622d))
* 🐛using jdbc client to fix ms database connect reset ([f4266c8](https://github.com/hks2002/docs-server-vertx/commit/f4266c8b0c3d560ff357a5ffa7f2d02715e06daa))


### Performance Improvements

* 🚀better code ([92b03e9](https://github.com/hks2002/docs-server-vertx/commit/92b03e986dc9c43d3acd7033f71c72df7eeb768f))
* 🚀better log for add water marker fail ([5ab20b9](https://github.com/hks2002/docs-server-vertx/commit/5ab20b92e6a7ec21d865e219fb643369f2915dc2))
* 🚀using HikariCP and JDBC instead of vertx DB ([8220f35](https://github.com/hks2002/docs-server-vertx/commit/8220f35696987184abef6995e8e11e6a487dca5a))


### Documentation

* 📚 add badges to readme ([59f16bf](https://github.com/hks2002/docs-server-vertx/commit/59f16bf23ffe07781460680bf95495c16fd48ab6))
* 📚 update readme ([cf10044](https://github.com/hks2002/docs-server-vertx/commit/cf10044528845f42cf6adbd15c706c3e318e71ba))
* 📚add LICENSE ([7cb8810](https://github.com/hks2002/docs-server-vertx/commit/7cb881074a6ad19227368ee04d986cb6e3d75590))
* 📚add notes for systemd service ([b55a2e8](https://github.com/hks2002/docs-server-vertx/commit/b55a2e82b4ad0923a438c9e06ca5f85835d37a98))
* 📚fix work flow status bug ([8abda65](https://github.com/hks2002/docs-server-vertx/commit/8abda65668de11d073e918b87a57ff0ae95a42f7))
* 📚update readme ([a37b4b7](https://github.com/hks2002/docs-server-vertx/commit/a37b4b7fc15a6cffbd341359f867bf1f90ca0327))
* 📚update readme ([600c8d5](https://github.com/hks2002/docs-server-vertx/commit/600c8d5c4cb344b71b99e1f7703ac9ab53f3181d))
