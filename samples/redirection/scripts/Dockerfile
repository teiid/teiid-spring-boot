FROM postgres:9.5
LABEL maintainer="https://lists.jboss.org/mailman/listinfo/teiid-users"

RUN mkdir -p /scripts
COPY example.sql /scripts

ENTRYPOINT ["psql",""]
