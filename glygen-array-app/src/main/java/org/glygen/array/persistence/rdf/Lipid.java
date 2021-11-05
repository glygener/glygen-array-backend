package org.glygen.array.persistence.rdf;

public class Lipid extends SmallMoleculeLinker {
    
    public Lipid() {
        this.type = LinkerType.LIPID;
    }
    
    public Lipid (SmallMoleculeLinker l) {
        this.classification = l.classification;
        this.name = l.name;
        //this.comment = l.comment;
        this.description = l.description;
        this.imageURL = l.imageURL;
        this.inChiKey = l.inChiKey;
        this.id = l.id;
        this.inChiSequence = l.inChiSequence;
        this.isomericSmiles = l.isomericSmiles;
        this.iupacName = l.iupacName;
        this.mass = l.mass;
        this.molecularFormula = l.molecularFormula;
        //this.opensRing = l.opensRing;
        this.pubChemId = l.pubChemId;
        this.publications = l.publications;
        this.smiles = l.smiles;
        this.uri = l.uri;
        this.type = LinkerType.LIPID;
    }

}
