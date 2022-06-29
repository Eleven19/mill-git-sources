package io.github.eleven19.mill.gitsource

import scala.util.control.NonFatal

import mill._
import mill.api.Result
import mill.scalalib._
import GitOps.GitCloneOptions

trait GitSourcesModule extends Module {
  def allGitSources = T.sources {gitSources().flatMap(p => os.walk(p.path)).map(PathRef(_))
}
  def shallowClone = T{ true }

  def gitSourcesRemoteUrl:String

  def gitSourcesRoot = T { millSourcePath / ".git_sources"}
  def gitSources = T.sources { getGitSources().repositoryPath}

  def gitCloneOptions = T{
    val remoteUrl = gitSourcesRemoteUrl
    if(shallowClone()) GitCloneOptions(repository = gitSourcesRemoteUrl, depth = Some(1), additionalArgs = Seq.empty)
    else GitCloneOptions(repository = gitSourcesRemoteUrl)
  }

  def getGitSources = T {
    val cloneOptions = gitCloneOptions()
    val gitSrcs = gitSourcesRoot()
    val envArgs = T.ctx.env
    GitOps.clone(gitSrcs, cloneOptions, envArgs)
  }
}
