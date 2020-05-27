job "nginx-internalapi" {
  region = "[[.region]]"
  datacenters = ["[[.datacenter]]"]
  type = "system"

//   constraint {
//     attribute = "${node.class}"
//     value     = "admin"
//   }

  group "nginx-internalapi" {
    task "nginx-internalapi" {
      driver = "docker"

      config {
        image = "nginx:[[.version]]"
        args  = [
          "nginx",
          "-c", "/local/nginx-internalapi.conf",
          "-g", "daemon off;",
        ]
        network_mode = "host"
      }

      template {
        data = <<EOF
upstream java_server_inner {
        server {{ env "NOMAD_IP_http" }}:8092;
        check interval=5000 rise=1 fall=3 timeout=4000;
}
upstream java_server_inner_v2 {
        server {{ env "NOMAD_IP_http" }}:8096;
        check interval=5000 rise=1 fall=3 timeout=4000;
}
upstream id_gene {
        server {{ env "NOMAD_IP_http" }}:8888;
        check interval=5000 rise=1 fall=3 timeout=4000;
}

server {
    listen       [[.server.port]];

    location ~  /(pinner|uinner|uegop|pegop|bjob|email|psgop|usgop)/ {
        proxy_set_header        Host            $host;
        proxy_set_header        X-Real-IP       $remote_addr;
        proxy_set_header        X-Forwarded-For $proxy_add_x_forwarded_for;

        proxy_pass http://java_server_inner;
    }

    location ^~ /v2/ {
        proxy_set_header        Host            $host;
        proxy_set_header        X-Real-IP       $remote_addr;
        proxy_set_header        X-Forwarded-For $proxy_add_x_forwarded_for;
        rewrite ^/v2/(.*) /$1 break;
        proxy_pass http://java_server_inner_v2;
    }

    location /api/snowflake {
        proxy_pass http://id_gene;
    }

    error_page  404              /404.html;
        location = /40x.html {
    }

    error_page   500 502 503 504  /50x.html;
        location = /50x.html {
    }
}
EOF
        destination   = "local/nginx-internalapi.conf"
        change_mode   = "signal"
        change_signal = "SIGHUP"
      }

      resources {
        cpu    = [[.cpu]]
        memory = [[.memory]]
        network {
          port "http" {
            static = 8080
          }
        }
      }
    }
  }
}
