# fastQC

Niassa-wrapped Cromwell (widdle) workflow for running FastQC tools on paired or unpaired reads.

![fastq flowchart](docs/fastqc-wf.png)
## Overview

## Dependencies

* [fastqc 0.11.9](https://www.bioinformatics.babraham.ac.uk/projects/fastqc/)


## Usage

### Cromwell
```
java -jar cromwell.jar run fastqc.wdl --inputs inputs.json
```

### Inputs

#### Required workflow parameters:
Parameter|Value|Description
---|---|---
`fastqR1`|File|Input file with the first mate reads.


#### Optional workflow parameters:
Parameter|Value|Default|Description
---|---|---|---
`fastqR2`|File?|None| Input file with the second mate reads (if not set the experiments will be regarded as single-end).
`outputFileNamePrefix`|String|""|Output prefix, customizable. Default is the first file's basename.
`r1Suffix`|String|"_R1"|Suffix for R1 file.
`r2Suffix`|String|"_R2"|Suffix for R2 file.


#### Optional task parameters:
Parameter|Value|Default|Description
---|---|---|---
`firstMateFastQC.jobMemory`|Int|6|Memory allocated to fastqc.
`firstMateFastQC.timeout`|Int|20|Timeout in hours, needed to override imposed limits.
`firstMateFastQC.javaHeap`|Int|4|Memory allocated to java heap, in G.
`firstMateFastQC.threads`|Int?|None|Threads param for fastqc
`firstMateFastQC.modules`|String|"perl/5.28 java/8 fastqc/0.11.8"|Names and versions of required modules.
`firstMateHtml.jobMemory`|Int|2|Memory allocated to this task.
`firstMateHtml.timeout`|Int|1|Timeout, in hours, needed to override imposed limits.
`firstMateZip.jobMemory`|Int|2|Memory allocated to this task.
`firstMateZip.timeout`|Int|1|Timeout, in hours, needed to override imposed limits.
`secondMateFastQC.jobMemory`|Int|6|Memory allocated to fastqc.
`secondMateFastQC.timeout`|Int|20|Timeout in hours, needed to override imposed limits.
`secondMateFastQC.javaHeap`|Int|4|Memory allocated to java heap, in G.
`secondMateFastQC.threads`|Int?|None|Threads param for fastqc
`secondMateFastQC.modules`|String|"perl/5.28 java/8 fastqc/0.11.8"|Names and versions of required modules.
`secondMateHtml.jobMemory`|Int|2|Memory allocated to this task.
`secondMateHtml.timeout`|Int|1|Timeout, in hours, needed to override imposed limits.
`secondMateZip.jobMemory`|Int|2|Memory allocated to this task.
`secondMateZip.timeout`|Int|1|Timeout, in hours, needed to override imposed limits.


### Outputs

Output | Type | Description
---|---|---
`html_report_R1`|File?|HTML report for the first mate fastq file.
`zip_bundle_R1`|File?|zipped report from FastQC for the first mate reads.
`html_report_R2`|File?|HTML report for read second mate fastq file.
`zip_bundle_R2`|File?|zipped report from FastQC for the second mate reads.


## Commands
 
 This section lists command(s) run by fastqc workflow
 
 * Running fastqc
 
 fastqc workflow runs the following command (excerpt from .wdl file). INPUT_FASTQ is a placeholder for an input file.
 
 ```
 
 FASTQC=$(which fastqc)
 JAVA=$(which java)
 perl $FASTQC INPUT_FASTQ --java=$JAVA --noextract --outdir "."
 
 ```
 
 ## Support

For support, please file an issue on the [Github project](https://github.com/oicr-gsi) or send an email to gsi@oicr.on.ca .

_Generated with generate-markdown-readme (https://github.com/oicr-gsi/gsi-wdl-tools/)_
