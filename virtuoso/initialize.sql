RDF_DEFAULT_USER_PERMS_SET ('nobody', 0);
RDF_GRAPH_USER_PERMS_SET ('http://glygen.org/glygenarray/public', 'nobody', 1);
RDF_GRAPH_GROUP_CREATE ('http://glygen.org/glygenarray/private', 1);
RDF_GRAPH_USER_PERMS_SET ('http://glygen.org/glygenarray/private', 'nobody', 0);
