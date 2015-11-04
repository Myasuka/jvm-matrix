# jvm-matrix
performance comparision of matrix library based on JVM

# Summary

I compare the performance of sparse matrix multiplication among four kinds of matrix library based on JVM:

1.  [SystemML](https://github.com/SparkTC/systemml): the matrix library used in SystemML
1.  [Breeze](https://github.com/scalanlp/breeze/): a numerical processing library for Scala
1.  [MTJ](https://github.com/fommil/matrix-toolkits-java/): Java linear algebra library powered by BLAS and LAPACK
1.  [ojAlgo](https://github.com/optimatika/ojAlgo): Open Source Java code that has to do with mathematics, linear algebra and optimisation.
 
A more general overview about Java Matrix benchmark can be found [here](http://lessthanoptimal.github.io/Java-Matrix-Benchmark/)

# How to use
As [SystemML](https://github.com/SparkTC/systemml) has not released any snapshot jar package on the repository, 
I append a complied jar package under ./lib folder.
Perhaps not anyone has the four library packages in the classpath, I recommend to assembly the jar package and then run applications.

**Note** As ojAlgo v38.2 requires jdk1.8, better use jdk1.8!

1.  run `sbt assembly` 
1.  `java -cp jvm-matrix-assembly-1.0.jar GenMat <rows> <cols> <sparsity> <output path>` 
1.  `java -cp jvm-matrix-assembly-1.0.jar SPMM <input A> <input B> <mode>` 
      
    mode 1 means use systemML sparse matrix multiplication,<br/>
    mode 2 means use Breeze sparse matrix multiplication,<br/> 
    mode 3 means use Breeze dense matrix multiplication, <br/> 
    mode 4 means use ojAlgo sparse matrix multiplication,<br/>
    mode 5 means use MTJ sparse matrix multiplication in `LinkedSparseMatrix` format,<br/>
    mode 6 means use MTJ sparse matrix multiplication in `CompColMatrix` format<br/>
    