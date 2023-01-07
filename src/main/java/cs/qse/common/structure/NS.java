package cs.qse.common.structure;

import java.util.List;

public class NS {
    String iri;
    String targetClass;
    Integer support;
    List<PS> propertyShapes;
    
    public String getIri() {
        return iri;
    }
    
    public void setIri(String iri) {
        this.iri = iri;
    }
    
    public String getTargetClass() {
        return targetClass;
    }
    
    public void setTargetClass(String targetClass) {
        this.targetClass = targetClass;
    }
    
    public Integer getSupport() {
        return support;
    }
    
    public void setSupport(Integer support) {
        this.support = support;
    }
    
    public List<PS> getPropertyShapes() {
        return propertyShapes;
    }
    
    public void setPropertyShapes(List<PS> propertyShapes) {
        this.propertyShapes = propertyShapes;
    }
}
