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

