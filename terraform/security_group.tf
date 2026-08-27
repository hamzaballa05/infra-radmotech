resource "openstack_networking_secgroup_v2" "radmotech_sg" {
  count       = 1  # désactivé tant que le quota Security Groups reste à 0/0
  name        = "radmotech-sg"
  description = "SSH + HTTP uniquement"
}

resource "openstack_networking_secgroup_rule_v2" "ssh" {
  count             = 1
  direction         = "ingress"
  ethertype         = "IPv4"
  protocol          = "tcp"
  port_range_min    = 22
  port_range_max    = 22
  remote_ip_prefix  = "${chomp(data.http.my_ip.response_body)}/32"
  security_group_id = openstack_networking_secgroup_v2.radmotech_sg[0].id
}

resource "openstack_networking_secgroup_rule_v2" "http" {
  count             = 1
  direction         = "ingress"
  ethertype         = "IPv4"
  protocol          = "tcp"
  port_range_min    = 80
  port_range_max    = 80
  remote_ip_prefix  = "0.0.0.0/0"
  security_group_id = openstack_networking_secgroup_v2.radmotech_sg[0].id
}
data "http" "my_ip" {
  url = "https://ifconfig.me/ip"
}