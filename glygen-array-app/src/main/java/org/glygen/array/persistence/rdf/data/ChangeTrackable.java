package org.glygen.array.persistence.rdf.data;

import java.util.List;

public interface ChangeTrackable {
    public List<ChangeLog> getChanges();
    public void setChanges(List<ChangeLog> changes);
    public void addChange (ChangeLog change);
}
