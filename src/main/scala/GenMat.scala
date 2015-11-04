import java.io.FileOutputStream

import com.ibm.bi.dml.runtime.util.FastBufferedDataOutputStream

import scala.util.Random

object GenMat {
  def main(args: Array[String]) {
    if(args.length < 4){
      println("usage: GenMat <rows> <cols> <sparsity> <output path>")
      System.exit(1)
    }
    val rows = args(0).toInt
    val cols = args(1).toInt
    val sparsity = args(2).toDouble
    val nnzPerRow = (cols * sparsity).toInt

    val output = args(3)
    val fos = new FileOutputStream(output)
    val out = new FastBufferedDataOutputStream(fos)
    out.writeInt(rows)
    out.writeInt(cols)
    out.writeByte(2)
    val tmp = rows.toLong * cols.toLong
    if( tmp > Int.MaxValue) {
      val nnz = (tmp * sparsity).toLong
      out.writeLong(nnz)
    }else {
      val nnz = (tmp * sparsity).toInt
      out.writeInt(nnz)
    }
    for(i <- 0 until rows){
      val colIndexes = Random.shuffle((0 until cols).toList).slice(0, nnzPerRow).sorted
      out.writeInt(colIndexes.size)
      for (j <- colIndexes){
        out.writeInt(j)
        out.writeDouble(Random.nextDouble())
      }
    }
    out.flush()
    out.close()
  }
}
