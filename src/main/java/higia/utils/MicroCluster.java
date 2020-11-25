package main.java.higia.utils;

import br.com.douglas444.datastreamenv.common.ConceptCategory;
import com.yahoo.labs.samoa.instances.DenseInstance;
import com.yahoo.labs.samoa.instances.Instance;

import java.util.List;

@SuppressWarnings("serial")
public class MicroCluster extends InstanceKernel{

	private String label;	  // label of the class associated to the micro-cluster. If it is a novelty the class is a sequential number
    private String type;  	  // category: normal, novelty or extension
    private long time;		      // timestamp associated to the last example classified in this micro-cluster
    private double threshold;
    private ConceptCategory conceptCategory;


    public MicroCluster(InstanceKernel element, String labelClasse, String category, long time,
						ConceptCategory conceptCategory){
    	super(element);
    	this.label = labelClasse;
    	this.type = category;
    	this.time = time;
    	this.threshold = 1.1;
    	this.conceptCategory = conceptCategory;
    }  
    
    public void insert(double element[], int timestamp){
    	Instance inst= new DenseInstance(1, element);	
    	super.insert(inst, timestamp);
    	this.setTime(timestamp);
    }
                
    public void setType(String category){
		this.type = category;
	}
            								
	public void setLabel(String labelClass){
			this.label = labelClass;
	}
            
	public String getType(){
		return type;
	}
            							
	public String getLabel(){
		return label;
	}

	public long getTime() {
		return time;
	}

	public void setTime(int time) {
		this.time = time;
	}

	public double getThreshold() {
		return threshold;
	}

	public void setThreshold(double threshold) {
		this.threshold = threshold;
	}

	public ConceptCategory getConceptCategory() {
		return conceptCategory;
	}

	public MicroCluster setConceptCategory(ConceptCategory conceptCategory) {
		this.conceptCategory = conceptCategory;
		return this;
	}

	public List<Instance> getInst() {
		return super.inst;
	}

}
		
		
