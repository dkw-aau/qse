package cs.qse.common.structure;

import java.util.List;

public class PS {
    String iri;
    String path;
    String nodeKind;
    
    String dataTypeOrClass;
    Boolean hasOrList;
    Integer support;
    Double confidence;
    List<ShaclOrListItem> shaclOrListItems;
    
    public String getIri() {
        return iri;
    }
    
    public void setIri(String iri) {
        this.iri = iri;
    }
    
    public String getPath() {
        return path;
    }
    
    public void setPath(String path) {
        this.path = path;
    }
    
    public String getNodeKind() {
        return nodeKind;
    }
    
    public void setNodeKind(String nodeKind) {
        this.nodeKind = nodeKind;
    }
    
    public String getDataTypeOrClass() {
        return dataTypeOrClass;
    }
    
    public void setDataTypeOrClass(String dataTypeOrClass) {
        this.dataTypeOrClass = dataTypeOrClass;
    }
    
    public Boolean getHasOrList() {
        return hasOrList;
    }
    
    public void setHasOrList(Boolean hasOrList) {
        this.hasOrList = hasOrList;
    }
    
    public Integer getSupport() {
        return support;
    }
    
    public void setSupport(Integer support) {
        this.support = support;
    }
    
    public Double getConfidence() {
        return confidence;
    }
    
    public void setConfidence(Double confidence) {
        this.confidence = confidence;
    }
    
    public List<ShaclOrListItem> getShaclOrListItems() {
        return shaclOrListItems;
    }
    
    public void setShaclOrListItems(List<ShaclOrListItem> shaclOrListItems) {
        this.shaclOrListItems = shaclOrListItems;
    }
    
}


