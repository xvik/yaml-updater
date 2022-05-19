FROM frolvlad/alpine-glibc:alpine-3.15


COPY upload/yaml-updater-linux-amd64 /usr/local/bin/yaml-updater

# ARG VERSION=1.4.1
# RUN set -x \
#   && wget -O /usr/local/bin/yaml-updater https://github.com/xvik/yaml-updater/releases/download/${VERSION}/yaml-updater-linux-amd64 \
#   && chmod +x /usr/local/bin/yaml-updater

ENTRYPOINT [ "/usr/local/bin/yaml-updater" ]
CMD ["--help"]
