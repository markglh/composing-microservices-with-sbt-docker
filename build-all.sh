#!/bin/bash

pushd aggregator-service
sbt clean docker
popd
pushd tracking-service
sbt clean docker
popd
pushd beacon-service
sbt clean docker
popd
