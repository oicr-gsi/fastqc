# fastqc

Niassa-wrapped Cromwell (widdle) workflow for running FastQC tools on paired or unpaired reads.

![fastq flowchart](docs/fastqc-wf.png)

## Running FastQC workflow

fastqc is a simple workflow that wraps FastQC tool. The expected inputs for this workflow - one or two compressed fastq files, depending on the protocol used (paired or unpaired reads).i The workflow produces an archive in .zip format which contains reports and metric files. Also, an html report is provisioned for each of the inputs.

### Parameters (Inputs) of the workflow

- fastqR1               input file with the first mate reads (required)
- fastqR2               input file with the second mate reads (optonal, if not set the experiments will be regarded as single-end)
- outputFileNamePrefix  output prefix, customizable. Default is the first file's basename
- r1Suffix              suffix for R1 file, default is \_R1
- r2Suffix              suffix for R2 file, default is \_R2
- jobMemory             memory, in GB
- modules               modules to use with fastqc processes
- timeout               timeout, in hours. Use when dealing with extra large inputs, default is 20h

### Provisioned outputs
Each fastqc run produces two types of outputs - an html report and a compressed archive with workflow's outputs.

Output | Type | Description
---|---|---
`resultZip`|File|All results from sequenza runs using gamma sweep.
`resultJson`|File|Combined json file with ploidy and contamination data.
`html_report_R1`|File?|First mate html report
`zip_bundle_R1`|File?|First mate archived report and data
`html_report_R2`|File?|Second mate html report
`zip_bundle_R2`|File?|Second mate archived report and data

## Niassa + Cromwell

This WDL workflow is wrapped in a Niassa workflow (https://github.com/oicr-gsi/pipedev/tree/master/pipedev-niassa-cromwell-workflow) so that it can used with the Niassa metadata tracking system (https://github.com/oicr-gsi/niassa).

* Building
```
mvn clean install
```

* Testing
```
mvn clean verify \
-Djava_opts="-Xmx1g -XX:+UseG1GC -XX:+UseStringDeduplication" \
-DrunTestThreads=2 \
-DskipITs=false \
-DskipRunITs=false \
-DworkingDirectory=/path/to/tmp/ \
-DschedulingHost=niassa_oozie_host \
-DwebserviceUrl=http://niassa-url:8080 \
-DwebserviceUser=niassa_user \
-DwebservicePassword=niassa_user_password \
-Dcromwell-host=http://cromwell-url:8000
```

## Support

For support, please file an issue on the [Github project](https://github.com/oicr-gsi) or send an email to gsi@oicr.on.ca.

