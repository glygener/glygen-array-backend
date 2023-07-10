package org.glygen.array.drs;

import java.util.List;

public class DrsObject {
    List<AccessMethod> accessMethods;   // required for single blobs, optional for bundles
    List<String> aliases;   // optional
    List<Checksum> cheksums;   // required
    List<ContentsObject> contents;   // required for bundles. if empty, this is a single blob
    String created_time;  // required
    String description;   // optional
    String id;    // required
    String mime_type = "application/json";    // optional
    String name;   //optional
    String self_uri;   // required
    Integer size;    // required
    String updated_time;   // optional
    String version;  //optional
}
