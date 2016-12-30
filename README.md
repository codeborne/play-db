play-db
=======

Play framework 1.4.x module for 
* lazy database connection
* DB operations logging

Add it to your dependencies.yml
-------------------------------

    require:
        - play
        - play-codeborne -> db 1.2
    
    repositories:
        - play-db:
          type: http
          artifact: https://repo.codeborne.com/play-db/[module]-[revision].zip
          contains:
            - play-codeborne -> *
