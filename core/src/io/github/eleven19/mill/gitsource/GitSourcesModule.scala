package io.github.eleven19.mill.gitsource

import scala.util.control.NonFatal

import mill._
import mill.api.Result
import mill.scalalib._
import GitOps.GitCloneOptions

trait GitSourcesModule extends Module {
  def shallowClone = T{ true }

  def gitSourcesRemoteUrl:String

  def gitSourcesRoot = T { millSourcePath / "git_files"}

  def gitCloneOptions = T{
    val remoteUrl = gitSourcesRemoteUrl
    if(shallowClone()) GitCloneOptions(remoteUrl = gitSourcesRemoteUrl, depth = Some(1), additionalArgs = Seq.empty)
    else GitCloneOptions(remoteUrl = gitSourcesRemoteUrl)
  }

  def getGitSources = T {
    val cloneOptions = gitCloneOptions()
    val gitSrcs = gitSourcesRoot()
    GitOps.clone(gitSrcs, cloneOptions)
  }
}
