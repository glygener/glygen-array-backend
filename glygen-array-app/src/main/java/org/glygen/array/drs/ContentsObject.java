package org.glygen.array.drs;

import java.util.List;

public class ContentsObject {

    List<ContentsObject> contents;     // optional
    List<String> drs_uri;    // optional
    String id;    // optional if inside another contents object
    String name;    // required
}
