--------------------------------------------------------------------------------------------
To execute glygen-array backend application.

1. set necessary environment variables:

    export GLYGEN_HOST='glycomics.ccrc.uga.edu'
    export GLYGEN_BASEPATH='/ggarray/api/'
    export JASYPT_SECRET='<jasypt_secret>'  (need to match with the one used for generating the database passwords)
    export SPRING_PROFILES_ACTIVE='dev'  (or defaults to 'prod)
    export SPRING_MAIL_HOST='40.97.120.34' .  (default is used if not specified)
    
 You can set any of the variables declared in docker-compose.xml file as an environment variable 
 if you need to use values other than defaults provided.
 
 2. Make sure postgres and virtuoso are up and running
 3. docker-compose up 
--------------------------------------------------------------------------------------------
