resource "openstack_compute_keypair_v2" "radmotech_key" {
  name       = "radmotech-key"
  public_key = var.ssh_public_key
}

resource "openstack_compute_instance_v2" "app" {
  name            = var.instance_name
  image_name      = var.image_name
  flavor_name     = var.flavor_name
  key_pair        = openstack_compute_keypair_v2.radmotech_key.name
  security_groups = [openstack_networking_secgroup_v2.radmotech_sg[0].name]
  user_data       = file("${path.module}/../scripts/bootstrap.sh")

  network {
    name = "Ext-Net"
  }
}