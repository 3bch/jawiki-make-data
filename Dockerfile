FROM 3bch/docker-workspace-dev
MAINTAINER 3bch

COPY ./script /root/script

CMD ["/root/script/run-all.sh"]

