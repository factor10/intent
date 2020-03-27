package intent.site

import intent.site.pages.Index

/**
 * Generate the Intent site and documentation to the docs path.
 */
@main def Main() =
  println("Generating documentation...")
  Seq(Index).map(p => writeFile(p.fileName(), p.generate()))

  def writeFile(fileName: String, content: String): Unit =
    import java.io._
    val filePath = java.nio.file.Paths.get("docs", fileName).toString
    val file = new File(filePath)
    val bw = new BufferedWriter(new FileWriter(file))
    bw.write(content)
    bw.close()
    println(s"${filePath}")
