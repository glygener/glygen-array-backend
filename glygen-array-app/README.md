--------------------------------------------------------------------------------------------
If this is your first time running the application:
1. Create a folder "glygen-array" under your user home directory
2. Create docker network

          docker network create glygen-array-network

To execute glygen-array backend application.

1. set necessary environment variables:

         export GLYGEN_HOST='glygen.ccrc.uga.edu'

         export GLYGEN_BASEPATH='/ggarray/api/'

         export JASYPT_SECRET='<jasypt_secret>'  (need to match with the one used for generating the database passwords)

         export SPRING_PROFILES_ACTIVE='dev'  (or defaults to 'prod)
    
    make sure environment variable HOME is also set to your user home directory
    
 You can set any of the variables declared in docker-compose.xml file as an environment variable 
 if you need to use values other than defaults provided.
 
 2. Make sure postgres and virtuoso are up and running
 
      a) to run postgres, go to glygen-array-backend/postgres directory
         set necessary environment variables:
         
         export POSTGRES_PASSWORD=<your-password>
         
         docker-compose up -d
         
      b) to run virtuoso, go to glygen-array-backend/virtuoso directory
         set necessary environment variables:
         
         export DBA_PASSWORD=<your-password>
         
         docker-compose up -d
         
 3. docker-compose up -d 
--------------------------------------------------------------------------------------------
