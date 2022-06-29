package io.github.eleven19.mill.gitsource

import mill.api.Result

object GitOps {
  final case class GitCloneOptions(remoteUrl:String, depth:Option[Int] = None, additionalArgs:Seq[String] = Seq.empty)
  object GitCloneOptions {
    implicit lazy val jsonify: upickle.default.ReadWriter[GitCloneOptions] = upickle.default.macroRW
  }

  final case class GitCloneResults(exitCode: Int, stdout: String, stderr: String)
  object GitCloneResults {
    implicit lazy val jsonify: upickle.default.ReadWriter[GitCloneResults] = upickle.default.macroRW
  }

  def clone(wd:os.Path, options:GitCloneOptions):Result[GitCloneResults] = {
    val results = GitCloneResults(0, "", "")
    results
  }
}
