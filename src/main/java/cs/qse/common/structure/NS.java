package cs.qse.common.structure;

import org.eclipse.rdf4j.model.IRI;

import java.util.List;

public class NS {
    IRI iri;
    IRI targetClass;
    Integer support;
    List<PS> propertyShapes;
    Integer countPropertyShapes;
    Boolean pruneFlag = false;
    Integer countPsWithPruneFlag;
    Integer countPscWithPruneFlag;
    
    public Integer getCountPsWithPruneFlag() {
        Integer count = 0;
        for (PS ps : this.getPropertyShapes()) {
            if (ps.getPruneFlag()) {
                count++;
            }
        }
        return count;
    }
    
    public Integer getCountPscWithPruneFlag() {
        Integer count = 0;
        for (PS ps : this.getPropertyShapes()) {
            if (ps.getShaclOrListItems() != null) {
                for (ShaclOrListItem item : ps.getShaclOrListItems()) {
                    if (item.getPruneFlag()) {
                        count++;
                    }
                }
            }
        }
        return count;
    }
    
    public Boolean getPruneFlag() {return pruneFlag;}
    
    public void setPruneFlag(Boolean pruneFlag) {this.pruneFlag = pruneFlag;}
    
    public IRI getIri() {
        return iri;
    }
    
    public void setIri(IRI iri) {
        this.iri = iri;
    }
    
    public String getLocalNameFromIri() {return iri.getLocalName();}
    
    public IRI getTargetClass() {
        return targetClass;
    }
    
    public void setTargetClass(IRI targetClass) {
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
    
    public Integer getCountPropertyShapes() {return propertyShapes.size();}
    
    public void setCountPropertyShapes() {countPropertyShapes = propertyShapes.size();}
}
