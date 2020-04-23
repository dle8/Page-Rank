//package com.company;

import java.io.IOException;
import java.io.*;
import java.net.URI;
import java.util.*;

import org.apache.hadoop.fs.Path;
import org.apache.hadoop.conf.*;
import org.apache.hadoop.io.*;
import org.apache.hadoop.mapred.*;
import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.util.*;
import org.apache.hadoop.filecache.DistributedCache;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.FSDataInputStream;
import org.apache.hadoop.fs.FSDataOutputStream;
import org.apache.hadoop.mapreduce.Mapper.Context;
import org.apache.hadoop.conf.Configuration;


public class Jacobi extends Configured implements Tool {
    public static final int max_iter = 100;
    public static final double eps = 1e-10;
    public static final String input_dir = "/users/input/jacobi/";
    public static String output_dir = "/users/dle8/output/";

    public static double converge(Configuration conf) throws IOException {
        FileSystem fs = FileSystem.get(conf);
        Path pre_file = new Path(output_dir + "pre_x/result");
        Path cur_file = new Path(output_dir + "cur_x/part-00000");
        if (!(fs.exists(pre_file) && fs.exists(cur_file))) {
            System.exit(1);
        }
        String line1, line2;
        InputStreamReader isr1 = new InputStreamReader(fs.open(pre_file));
        InputStreamReader isr2 = new InputStreamReader(fs.open(cur_file));
        BufferedReader br1 = new BufferedReader(isr1);
        BufferedReader br2 = new BufferedReader(isr2);

        // Check for convergence condition by comparing previous and current values of every X[i]
        double diff = 0.0;
        while ((line1 = br1.readLine()) != null && (line2 = br2.readLine()) != null) {
            String[] str1 = line1.split("\\s+");
            String[] str2 = line2.split("\\s+");
            double pre_x = Double.parseDouble(str1[1]);
            double cur_x = Double.parseDouble(str2[1]);
            if (Math.abs(pre_x - cur_x) > eps) {
                diff = Math.abs(pre_x - cur_x);
                break;
            }
        }

        return diff;
    }

    public static class Map extends MapReduceBase implements Mapper<LongWritable, Text, IntWritable, DoubleWritable> {
        private double[] sum, res, diagonal;
        int n;

        public void configure(JobConf conf) {
            try{
                n = conf.getInt("N", 0);

                sum = new double[n];
                Arrays.fill(sum, 0);
                res = new double[n];
                Arrays.fill(res, 0);
                diagonal = new double[n];
                Arrays.fill(diagonal, 0);

                URI[] input_URIs = DistributedCache.getCacheFiles(conf);

                Path input_path = new Path(input_URIs[0].getPath());
                FileSystem fs = FileSystem.get(URI.create(input_path.getName()), conf);
                FSDataInputStream fis = fs.open(input_path);
                String line = null;
                while ((line = fis.readLine()) != null) {
                    StringTokenizer tokenizer = new StringTokenizer(line);
                    int rowIdx = Integer.parseInt(tokenizer.nextToken());
                    int colIdx = Integer.parseInt(tokenizer.nextToken());
                    double val = Double.parseDouble(tokenizer.nextToken());
                    if (rowIdx == colIdx) diagonal[rowIdx] = val;
                    else if (colIdx == n) res[rowIdx] = val;
                    else sum[rowIdx] += val;
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        public void map(LongWritable key, Text value, OutputCollector<IntWritable, DoubleWritable> output, Reporter reporter) throws IOException {
            String line = value.toString();
            StringTokenizer tokenizer = new StringTokenizer(line);
            int rowIdx = 0;
            double xValue = 0;
            if (tokenizer.hasMoreTokens()) {
                rowIdx = Integer.parseInt(tokenizer.nextToken());
                xValue = Double.parseDouble(tokenizer.nextToken());
            }
            double xResult = (res[rowIdx] - (sum[rowIdx] * xValue)) / diagonal[rowIdx];
            output.collect(new IntWritable(rowIdx), new DoubleWritable(xResult));
        }
    }

    public static class Reduce extends MapReduceBase implements Reducer<IntWritable, DoubleWritable, IntWritable, DoubleWritable> {
        @Override
        public void reduce(IntWritable key, Iterator<DoubleWritable> values, OutputCollector<IntWritable, DoubleWritable> output, Reporter reporter) throws IOException {
            output.collect(key, values.next());
        }
    }

    @Override
    public int run(String[] args) throws Exception {
        Configuration conf = getConf();
        FileSystem fs = FileSystem.get(conf);
        JobConf job = new JobConf(conf);
        Path curFile = new Path(output_dir + "cur_x");
        if (fs.exists(curFile)) {
            fs.delete(new Path(output_dir + "pre_x"), true);
            fs.rename(new Path(output_dir + "cur_x"), new Path(output_dir + "pre_x"));
            fs.rename(new Path(output_dir + "pre_x/part-00000"), new Path(output_dir + "pre_x/result"));
        }
        job.setJarByClass(Jacobi.class);
        job.setMapperClass(Map.class);
        job.setNumMapTasks(Integer.parseInt(args[2]));
        job.setReducerClass(Reduce.class);
        // Must be one
        job.setNumReduceTasks(Integer.parseInt(args[3]));
        job.setOutputKeyClass(IntWritable.class);
        job.setOutputValueClass(DoubleWritable.class);
        job.setInputFormat(TextInputFormat.class);
        job.setOutputFormat(TextOutputFormat.class);

        FileInputFormat.setInputPaths(job, new Path(output_dir + "pre_x"));
        FileOutputFormat.setOutputPath(job, new Path(output_dir + "cur_x"));
        JobClient.runJob(job);
        return 1;
    }

    public static void main(String[] args) throws Exception {
        Configuration conf = new Configuration();
        FileSystem fs = FileSystem.get(conf);
        Path matFile = new Path(args[0]);
        String file_name = matFile.getName();
        int n = Integer.parseInt(file_name.substring(0, file_name.lastIndexOf('.')));
        conf.setInt("N", n);

        // Remove existing dir whose name equals to output dir
        output_dir = args[1];
        if (!output_dir.endsWith("/")) {
            output_dir += "/";
        }
        Path out_dir = new Path(output_dir);
        if (fs.exists(out_dir)) fs.delete(out_dir);

        // Initialize all values of vector X with zero and write to output directory
        // Each line contains the line number i and values X[i]
        Path xFile = new Path(output_dir + "pre_x/result");
        FSDataOutputStream xData = fs.create(xFile);
        BufferedWriter x = new BufferedWriter(new OutputStreamWriter(xData));
        for (int i = 0; i < n; ++i) {
            x.write(i + " 0");
            x.newLine();
        }
        x.close();

        DistributedCache.addCacheFile(new URI(args[0]), conf);

        int i = 0;
        for (; i < max_iter; ++i) {
            ToolRunner.run(conf, new Jacobi(), args);
            double diff = converge(conf);
//            System.out.println("Iteration = " + (i + 1));
//            System.out.println("Different = " + diff);
//            System.out.flush();

            // Converge if the difference if below eps
            if (diff < eps) break;
        }
        System.out.println("Exit after " + i + " iterations");
    }
}