package io.github.eleven19.mill.gitsource

import mill._
import mill.api.Result
import mill.modules.Jvm

object GitOps {
  lazy val gitExe = if(scala.util.Properties.isWin) "git.exe" else "git"
  final case class GitCloneOptions(repository:String, depth:Option[Int] = None, additionalArgs:Seq[String] = Seq.empty) {
    def toArgs:Seq[String] = Seq("clone") ++ depth.map(d => s"--depth=$d").toSeq ++ additionalArgs ++ Seq(repository)
  }
  object GitCloneOptions {
    implicit lazy val jsonify: upickle.default.ReadWriter[GitCloneOptions] = upickle.default.macroRW
  }

  final case class GitCloneResults(repositoryPath:os.Path, hash:String)
  object GitCloneResults {
    implicit lazy val jsonify: upickle.default.ReadWriter[GitCloneResults] = upickle.default.macroRW
  }

  def clone(wd:os.Path, options:GitCloneOptions, envArgs:Map[String,String] = Map.empty) = {
    os.makeDir.all(wd)
    val command = Vector(gitExe) ++ options.toArgs
    val res = os.proc(command).call( cwd = wd, env = envArgs)
    val repositoryPath = os.list(wd).head
    val hash = getHash(repositoryPath)
    GitCloneResults(repositoryPath, hash)
  }

  def getHash(repositoryPath:os.Path) = {
    val lsFilesCommand = Vector(gitExe, "ls-files", "-s", repositoryPath.toString())
    val lsFiles = os.proc(lsFilesCommand).spawn(stderr = os.Inherit)
    val hashObjectCommand = Vector(gitExe, "hash-object", "--stdin")
    val hashObject = os.proc(hashObjectCommand).call(stdin = lsFiles.stdout)
    // val command = Vector(gitExe, "rev-parse", "HEAD")
    // val res = os.proc(command).call(cwd = repositoryPath)
    hashObject.out.trim()
  }
}
