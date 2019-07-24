# nano-prometheus-exporter

Aims to export metrics from the [Nano Node](https://github.com/nanocurrency/nano-node) to prometheus.

## Build and run

This project uses sbt with the native packager for docker builds.

### Run locally

    $ sbt run
    
### To build docker container (to link with the Nano Docker container)

    $ sbt docker:publishLocal
    
    
## Environment variables

* `NANO_HOST` - The URL of the Node api (defaults to `http://localhost:7076`)
* `NANO_ADDRESS` - A specific node address to follow representative status of
    