# Analysis


## Setup notebook


   ```bash
   docker pull jupyter/datascience-notebook
   ```


  ```bash
  cd notebooks

  docker run  --name exp_notebook --rm -p 8888:8888 -e JUPYTER_ENABLE_LAB=yes -v "$PWD":/home/jovyan jupyter/datascience-notebook
  ```
