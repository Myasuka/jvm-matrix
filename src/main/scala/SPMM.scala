import java.io.{BufferedWriter, FileOutputStream, EOFException, FileInputStream}

import breeze.linalg.{DenseMatrix => BDM, CSCMatrix}
import com.ibm.bi.dml.runtime.matrix.data.{LibMatrixMult, MatrixBlock}
import com.ibm.bi.dml.runtime.util.{FastBufferedDataOutputStream, FastBufferedDataInputStream, LocalFileUtils}
import no.uib.cipr.matrix.DenseMatrix
import no.uib.cipr.matrix.sparse.{CompColMatrix, LinkedSparseMatrix}
import org.ojalgo.OjAlgoUtils
import org.ojalgo.machine.{BasicMachine, Hardware}
import org.ojalgo.matrix.{BasicMatrix, PrimitiveMatrix}

object SPMM {


  def main(args: Array[String]) {
    if (args.length < 3) {
      println("usage: SPMM <input A> <input B> <mode>")
      println("       mode 1 means use systemML sparse matrix multiplication")
      println("       mode 2 means use Breeze sparse matrix multiplication")
      println("       mode 3 means use Breeze dense matrix multiplication")
      println("       mode 4 means use ojAlgo sparse matrix multiplication")
      println("       mode 5 means use MTJ sparse matrix multiplication in `LinkedSparseMatrix` format")
      println("       mode 6 means use MTJ sparse matrix multiplication in `CompColMatrix` format")
      System.exit(1)
    }
    val inputA = args(0)
    val inputB = args(1)
    val mode = args(2)
    mode match {
      case "1" =>
        println("==========================")
        println("test SystemML spMM")
        val sysMatA = LocalFileUtils.readMatrixBlockFromLocal(inputA)
        val sysMatB = LocalFileUtils.readMatrixBlockFromLocal(inputA)
        // TODO here we take the result matrix as not sparse matrix
        val sysMatC = new MatrixBlock(sysMatA.getNumRows, sysMatB.getNumColumns, false)
        println("finish preparing input matrices!")
        val t = System.currentTimeMillis()
        // TODO need to think about the parallelism about sparse matrix multiplication
        LibMatrixMult.matrixMult(sysMatA, sysMatB, sysMatC)
        println(s"systemML spMM used time: ${System.currentTimeMillis() - t} mills")
        if (args.length == 3)
          println(s"result(1, 1): ${sysMatC.getValue(1, 1)}")
        else {
          val r = args(3).toInt
          val c = args(4).toInt
          println(s"result($r, $c): ${sysMatC.getValue(r, c)}")
        }
        if (args.length > 5)
          sysMatC.write(new FastBufferedDataOutputStream(new FileOutputStream(args(5))))
      case "2" =>
        println("==========================")
        println("test Breeze spMM")
        val brzMatA = readBrzCSCmat(inputA)
        val brzMatB = readBrzCSCmat(inputB)
        println("finish preparing input matrices!")
        //        println(s"breeze matrix A, rows: ${brzMatA.rows}, cols: ${brzMatA.cols}, nnz: ${brzMatA.activeSize}")
//        println(s"breeze matrix B, rows: ${brzMatB.rows}, cols: ${brzMatB.cols}, nnz: ${brzMatB.activeSize}")
        val t = System.currentTimeMillis()
        val result = brzMatA * brzMatB
        println(s"breeze spMM used time: ${System.currentTimeMillis() - t} mills")
        println(s"first active result: ${result.activeIterator.next.toString()}")
        if (args.length == 3)
          println(s"result(1, 1): ${result(1, 1)}")
        else {
          val r = args(3).toInt
          val c = args(4).toInt
          println(s"result($r, $c): ${result(r, c)}")
        }
      case "3" =>
        println("==========================")
        println("test Breeze dGeMM")
        val r = args(3).toInt
        val c = args(4).toInt
        val brzMatA = readBrzCSCmat(inputA).toDenseMatrix
        val brzMatB = readBrzCSCmat(inputB).toDenseMatrix
        println("finish preparing input matrices!")
        val t = System.currentTimeMillis()
        val result: BDM[Double] = brzMatA * brzMatB
        println(s"breeze dGeMM used time: ${System.currentTimeMillis() - t} mills")
        println(s"result($r, $c): ${result(r, c)}")
      case "4" =>
        println("==========================")
        println("test ojAlgo mm")
        val tmpL1Machine = new BasicMachine(32L * 1024L, 1) //No Hyperthreading

        val tmpL2Machine = new BasicMachine(256L * 1024L, tmpL1Machine.threads)

        val tmpL3Machine = new BasicMachine(3L * 1024L * 1024L, 1)

        val tmpSystemMachine = new BasicMachine(3L * 1024L * 1024L * 1024L, 1)
        val machines = Array(tmpSystemMachine, tmpL3Machine, tmpL2Machine, tmpL1Machine)
        val xeon = new Hardware("xeon_E5620", machines)
        OjAlgoUtils.ENVIRONMENT =  xeon.virtualise()
        println(s"use my own hardware: availableMemory ${OjAlgoUtils.ENVIRONMENT.getAvailableMemory}, " +
          s"architecture: ${OjAlgoUtils.ENVIRONMENT.architecture}, cores: ${OjAlgoUtils.ENVIRONMENT.cores}, " +
          s"threads: ${OjAlgoUtils.ENVIRONMENT.threads}")
        println(OjAlgoUtils.ENVIRONMENT.toString)

        val ojMatA = readOJmat(inputA)
        val ojMatB = readOJmat(inputB)
        val t = System.currentTimeMillis()
        println("finish preparing input matrices!")
        val result = ojMatA.multiply(ojMatB)
        println(s"ojAlgo MM used time: ${System.currentTimeMillis() - t} mills")
        if (args.length == 3)
          println(s"result(1, 1): ${result.get(1, 1)}")
        else {
          val r = args(3).toInt
          val c = args(4).toInt
          println(s"result($r, $c): ${result.get(r, c)}")
        }
      case "5" =>
        println("==========================")
        println("test mtj mm LinkedSparseMatrix")
        val mtjMatA = readMTJmat(inputA)
        val mtjMatB = readMTJmat(inputB)
        val mtjMatC = new DenseMatrix(mtjMatA.numRows(), mtjMatB.numColumns())
        mtjMatC.zero()
        println(s"matA(1,1): ${mtjMatA.get(1, 1)}, matB(1,1): ${mtjMatB.get(1, 1)},")
        println("finish preparing input matrices!")
        val t = System.currentTimeMillis()
        mtjMatA.mult(mtjMatB, mtjMatC)
        println(s"MTJ MM used time: ${System.currentTimeMillis() - t} mills")
        if (args.length == 3)
          println(s"result(1, 1): ${mtjMatC.get(1, 1)}")
        else {
          val r = args(3).toInt
          val c = args(4).toInt
          println(s"result($r, $c): ${mtjMatC.get(r, c)}")
        }
      case "6" =>
        println("==========================")
        println("test mtj mm CompColMatrix")
        val mtjMatA = new CompColMatrix(readMTJmat(inputA), true)
        val mtjMatB = new CompColMatrix(readMTJmat(inputB), true)
        val mtjMatC = new DenseMatrix(mtjMatA.numRows(), mtjMatB.numColumns())
        mtjMatC.zero()
        println("finish preparing input matrices!")
        println(s"matA(1,1): ${mtjMatA.get(1, 1)}, matB(1,1): ${mtjMatB.get(1, 1)},")
        val t = System.currentTimeMillis()
        mtjMatA.mult(mtjMatB, mtjMatC)
        println(s"MTJ MM used time: ${System.currentTimeMillis() - t} mills")
        if (args.length == 3)
          println(s"result(1, 1): ${mtjMatC.get(1, 1)}")
        else {
          val r = args(3).toInt
          val c = args(4).toInt
          println(s"result($r, $c): ${mtjMatC.get(r, c)}")
        }
    }


  }

  def readBrzCSCmat(input: String): CSCMatrix[Double] = {
    val fis = new FileInputStream(input)
    val in = new FastBufferedDataInputStream(fis, 8192)
    val rows = in.readInt()
    val cols = in.readInt()
    in.readByte()
    val nnz = if (rows.toLong * cols.toLong > Int.MaxValue) in.readLong() else in.readInt()
    println(s"non-zeros: $nnz")
    val builder = new CSCMatrix.Builder[Double](rows, cols)
    for (r <- 0 until rows) {
      val nr = in.readInt()
      for (c <- 0 until nr) {
        val c = in.readInt()
        val v = in.readDouble()
        builder.add(r, c, v)
      }
    }
    in.close()
    builder.result
  }

  def readOJmat(input: String): BasicMatrix = {
    val fis = new FileInputStream(input)
    val in = new FastBufferedDataInputStream(fis, 8192)
    val rows = in.readInt()
    val cols = in.readInt()
    in.readByte()
    val nnz = if (rows.toLong * cols.toLong > Int.MaxValue) in.readLong() else in.readInt()
    val builder = PrimitiveMatrix.getBuilder(rows, cols)
    for (r <- 0 until rows) {
      val nr = in.readInt()
      for (c <- 0 until nr) {
        val c = in.readInt()
        val v = in.readDouble()
        builder.set(r, c, v)
      }
    }
    builder.build()
  }

  def readMTJmat(input: String): LinkedSparseMatrix = {
    val fis = new FileInputStream(input)
    val in = new FastBufferedDataInputStream(fis, 8192)
    val rows = in.readInt()
    val cols = in.readInt()
    in.readByte()
    val nnz = if (rows.toLong * cols.toLong > Int.MaxValue) in.readLong() else in.readInt()
    val mat = new LinkedSparseMatrix(rows, cols)
    for (r <- 0 until rows) {
      val nr = in.readInt()
      for (c <- 0 until nr) {
        val c = in.readInt()
        val v = in.readDouble()
        mat.set(r, c, v)
      }
    }
    mat
  }


}
