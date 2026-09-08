resource "openstack_compute_keypair_v2" "radmotech_key" {
  name       = "radmotech-key"
  public_key = var.ssh_public_key
}
resource "random_password" "db_password" {
  length  = 24
  special = false
}
resource "openstack_compute_instance_v2" "app" {
  name            = var.instance_name
  image_name      = var.image_name
  flavor_name     = var.flavor_name
  key_pair        = openstack_compute_keypair_v2.radmotech_key.name
  security_groups = [openstack_networking_secgroup_v2.radmotech_sg[0].name]
  user_data = templatefile("${path.module}/../scripts/bootstrap.sh.tpl", {
    db_password = random_password.db_password.result
})
  network {
    name = "Ext-Net"
  }
}
