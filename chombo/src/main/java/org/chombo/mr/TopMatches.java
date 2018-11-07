/*
 * chombo: Hadoop Map Reduce utility
 * Author: Pranab Ghosh
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you
 * may not use this file except in compliance with the License. You may
 * obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0 
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or
 * implied. See the License for the specific language governing
 * permissions and limitations under the License.
 */


package org.chombo.mr;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.conf.Configured;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.LongWritable;
import org.apache.hadoop.io.NullWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.mapreduce.Mapper;
import org.apache.hadoop.mapreduce.Reducer;
import org.apache.hadoop.mapreduce.lib.input.FileInputFormat;
import org.apache.hadoop.mapreduce.lib.output.FileOutputFormat;
import org.apache.hadoop.util.Tool;
import org.apache.hadoop.util.ToolRunner;
import org.chombo.util.BasicUtils;
import org.chombo.util.SecondarySort;
import org.chombo.util.Tuple;
import org.chombo.util.Utility;

/**
 * Top match map reduce based on distance with neighbors
 * @author pranab
 *
 */
public class TopMatches extends Configured implements Tool {

	@Override
	public int run(String[] args) throws Exception {
        Job job = new Job(getConf());
        String jobName = "Top n matches MR";
        job.setJobName(jobName);
        
        job.setJarByClass(TopMatches.class);
        
        FileInputFormat.addInputPath(job, new Path(args[0]));
        FileOutputFormat.setOutputPath(job, new Path(args[1]));
        
        job.setMapperClass(TopMatches.TopMatchesMapper.class);
        job.setReducerClass(TopMatches.TopMatchesReducer.class);
        job.setCombinerClass(TopMatches.TopMatchesCombiner.class);
        
        job.setMapOutputKeyClass(Tuple.class);
        job.setMapOutputValueClass(Text.class);

        job.setOutputKeyClass(NullWritable.class);
        job.setOutputValueClass(Text.class);
        
        job.setGroupingComparatorClass(SecondarySort.TuplePairGroupComprator.class);
        job.setPartitionerClass(SecondarySort.TupleTextPartitioner.class);

        Utility.setConfiguration(job.getConfiguration());
        int numReducer = job.getConfiguration().getInt("tom.num.reducer", -1);
        numReducer = -1 == numReducer ? job.getConfiguration().getInt("num.reducer", 1) : numReducer;
        job.setNumReduceTasks(numReducer);
        
        int status =  job.waitForCompletion(true) ? 0 : 1;
        return status;
	}
	
	/**
	 * @author pranab
	 *
	 */
	public static class TopMatchesMapper extends Mapper<LongWritable, Text, Tuple, Text> {
		private String srcEntityId;
		private String trgEntityId;
		private int rank;
		private Tuple outKey = new Tuple();
		private Text outVal = new Text();
        private String fieldDelimRegex;
        private String fieldDelim;
        private int distOrdinal;
        private boolean recordInOutput;
        private int recLength = -1;
        private String srcRec;
        private String trgRec;
        private int srcRecBeg;
        private int srcRecEnd;
        private int trgRecBeg;
        private int trgRecEnd;
        private String[] items;

        /* (non-Javadoc)
         * @see org.apache.hadoop.mapreduce.Mapper#setup(org.apache.hadoop.mapreduce.Mapper.Context)
         */
        protected void setup(Context context) throws IOException, InterruptedException {
			Configuration conf = context.getConfiguration();
           	fieldDelim = conf.get("field.delim", ",");
            fieldDelimRegex = conf.get("field.delim.regex", ",");
            distOrdinal = conf.getInt("tom.distance.ordinal", -1);
        	recordInOutput =  conf.getBoolean("tom.record.in.output", false);     
        }    

        /* (non-Javadoc)
         * @see org.apache.hadoop.mapreduce.Mapper#map(KEYIN, VALUEIN, org.apache.hadoop.mapreduce.Mapper.Context)
         */
        @Override
        protected void map(LongWritable key, Text value, Context context)
            throws IOException, InterruptedException {
            items  =  value.toString().split(fieldDelimRegex);
            
            srcEntityId = items[0];
            trgEntityId = items[1];
            rank = Integer.parseInt(items[items.length - 1]);
            
            outKey.initialize();
            if (recordInOutput) {
            	//include source and target record
            	if (recLength == -1) {
            		recLength = (items.length - 3) / 2;
            		srcRecBeg = 2;
            		srcRecEnd =  trgRecBeg = 2 + recLength;
            		trgRecEnd = trgRecBeg + recLength;
            	}
            	srcRec = BasicUtils.join(items, srcRecBeg, srcRecEnd, fieldDelim);
            	trgRec = BasicUtils.join(items, trgRecBeg, trgRecEnd, fieldDelim);
            	outKey.add(srcEntityId, srcRec, rank);
            	outVal.set(trgEntityId +  fieldDelim + trgRec + fieldDelim + items[items.length - 1]);            	
            } else {
            	//only target entity id and distance
            	outKey.add(srcEntityId, rank);
            	outVal.set(trgEntityId + fieldDelim + items[items.length - 1]);
            }
 			context.write(outKey, outVal);
        }
	}

    /**
     * @author pranab
     *
     */
    public static class TopMatchesCombiner extends Reducer<Tuple, Text, Tuple, Text> {
    	private boolean nearestByCount;
    	private int topMatchCount;
    	private int topMatchDistance;
		private int count;
		private int distance;
        private String fieldDelim;
        private boolean emitTargetEntityIdOnly;
        private Text outVal = new Text();
        private String targetEntityId;
        
        /* (non-Javadoc)
         * @see org.apache.hadoop.mapreduce.Reducer#setup(org.apache.hadoop.mapreduce.Reducer.Context)
         */
        protected void setup(Context context) throws IOException, InterruptedException {
			Configuration conf = context.getConfiguration();
           	fieldDelim = conf.get("field.delim", ",");
        	nearestByCount = conf.getBoolean("tom.nearest.by.count", true);
        	if (nearestByCount) {
        		topMatchCount = conf.getInt("tom.match.count", 10);
        	} else {
        		topMatchDistance = conf.getInt("tom.match.distance", 200);
        	}
        	emitTargetEntityIdOnly = conf.getBoolean("tom.emit.target.entityId.only", false);
       }
        
       	/* (non-Javadoc)
    	 * @see org.apache.hadoop.mapreduce.Reducer#reduce(KEYIN, java.lang.Iterable, org.apache.hadoop.mapreduce.Reducer.Context)
    	 */
    	protected void reduce(Tuple key, Iterable<Text> values, Context context)
        	throws IOException, InterruptedException {
    		count = 0;
        	for (Text value : values){
        		//count based neighbor
				if (nearestByCount) {
					context.write(key, value);
	        		if (++count == topMatchCount){
	        			break;
	        		}
				} else {
					//distance based neighbor
					distance =Integer.parseInt( value.toString().split(fieldDelim)[1]);
					if (distance  <=  topMatchDistance ) {
						context.write(key, value);
					} else {
						break;
					}
				}
        	}
    	}
    }
    
    
    /**
     * @author pranab
     *
     */
    public static class TopMatchesReducer extends Reducer<Tuple, Text, NullWritable, Text> {
    	private boolean nearestByCount;
    	private boolean nearestByDistance;
    	private int topMatchCount;
    	private int topMatchDistance;
		private String srcEntityId;
		private int count;
		private int distance;
		private Text outVal = new Text();
        private String fieldDelim;
        private boolean recordInOutput;
        private boolean compactOutput;
        private List<String> valueList = new ArrayList<String>();
    	private boolean outputWithNoNeighbor;
        private boolean emitTargetEntityIdOnly;
        private String targetEntityId;
        private boolean outputNumNeighbors;
		private StringBuilder stBld =  new StringBuilder();
		private String srcRec;

        /* (non-Javadoc)
         * @see org.apache.hadoop.mapreduce.Reducer#setup(org.apache.hadoop.mapreduce.Reducer.Context)
         */
        protected void setup(Context context) throws IOException, InterruptedException {
			Configuration conf = context.getConfiguration();
           	fieldDelim = conf.get("field.delim", ",");
        	nearestByCount = conf.getBoolean("tom.nearest.by.count", true);
        	nearestByDistance = conf.getBoolean("tom.nearest.by.distance", false);
        	if (nearestByCount) {
        		topMatchCount = conf.getInt("tom.top.match.count", 10);
        	} else {
        		topMatchDistance = conf.getInt("tom.top.match.distance", 200);
        	}
        	recordInOutput =  conf.getBoolean("tom.record.in.output", false);     
        	compactOutput =  conf.getBoolean("tom.compact.output", false);     
        	outputWithNoNeighbor =  conf.getBoolean("tom.output.with.no.neighbor", false);  
        	outputNumNeighbors = conf.getBoolean("tom.output.num.neighbors", true);
        }
    	
    	/* (non-Javadoc)
    	 * @see org.apache.hadoop.mapreduce.Reducer#reduce(KEYIN, java.lang.Iterable, org.apache.hadoop.mapreduce.Reducer.Context)
    	 */
    	protected void reduce(Tuple key, Iterable<Text> values, Context context)
        	throws IOException, InterruptedException {
    		srcEntityId  = key.getString(0);
    		srcRec = recordInOutput ? key.getString(1) : "";
    		count = 0;
    		boolean doEmitNeighbor = false;
    		valueList.clear();
        	for (Text value : values){
        		doEmitNeighbor = false;
        		
        		//only count based neighbor
				if (nearestByCount && !nearestByDistance) {
					++count;
					doEmitNeighbor = count >= topMatchCount ? false : true;
				} 
				
				//distance based neighbors
				if (nearestByDistance) {
					//distance based neighbor
					String[] items = value.toString().split(fieldDelim);
					distance = Integer.parseInt(items[items.length - 1]);
					if (distance  <=  topMatchDistance ) {
						if (!nearestByCount) {
							//only distance based
							doEmitNeighbor = true;
						} else {
							//apply count in addition to distance
							++count;
							doEmitNeighbor = count >= topMatchCount ? false : true;
						}
					} else {
						doEmitNeighbor = false;
					}
				}
				
				if (doEmitNeighbor) {
					if (compactOutput) {
						//one line output
						if (recordInOutput) {
							//contains id,record,rank - strip out entity ID and rank
							valueList.add(extractTargetRec(value));
						} else {
							//contains id, rank : strip out rank
							valueList.add(value.toString().split(fieldDelim)[0]);
						}
					} else {
						//multi line output
						if (recordInOutput) {
							//two records
							outVal.set(srcRec + fieldDelim + extractTargetRec(value));
						} else {
							//two IDs
							outVal.set(srcEntityId + fieldDelim + value.toString().split(fieldDelim)[0]);
						}
						context.write(NullWritable.get(), outVal);
					}
				} else {
					//only source  if neighborhood condition not met
					if (outputWithNoNeighbor && !compactOutput) {
						outVal.set(recordInOutput ? srcRec : srcEntityId);
						context.write(NullWritable.get(), outVal);
					}
				}
        	}
        	
        	//emit in compact format
        	if (compactOutput) {
        		boolean doEmit = true;
        		int numNeighbor = valueList.size();
        		if ( 0 == numNeighbor) {
					//only source entity if neighborhood condition not met
        			if (outputWithNoNeighbor) {
        				//output even with no neighbor 
        				outVal.set(recordInOutput ? srcRec : srcEntityId);
        			} else {
        				doEmit = false;
        			}
        		} else {
        			stBld.delete(0, stBld.length());
        			String targetValues = BasicUtils.join(valueList, fieldDelim);
        			if (recordInOutput) {
        				//source record followed by list of target records
	        			stBld.append(srcRec).append(fieldDelim);
	        			if (outputNumNeighbors) {
	            			stBld.append(numNeighbor).append(fieldDelim);
	        			}
	        			stBld.append(targetValues);
        			} else {
        				//source ID followed by list target IDs
        				stBld.append(srcEntityId).append(fieldDelim).append(targetValues);
        			}
        			outVal.set(stBld.toString());
        		}
        		if (doEmit) {
        			context.write(NullWritable.get(), outVal);
        		}
        	}
    	}
    	
    	/**
    	 * @param value
    	 * @return
    	 */
    	private String extractTargetRec(Text value) {
    		//strip out entity ID and rank
			String[] valueItems = value.toString().split(fieldDelim);
			return BasicUtils.join(valueItems, 1, valueItems.length -1);
    	}
    	
    }
	
	/**
	 * @param args
	 * @throws Exception
	 */
	public static void main(String[] args) throws Exception {
        int exitCode = ToolRunner.run(new TopMatches(), args);
        System.exit(exitCode);
	}
}
